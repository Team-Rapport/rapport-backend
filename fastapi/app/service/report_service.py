"""
report_service.py — 하이브리드 점수 산출 (키워드 40% + LLM 60%)

역할:
1. analyzer.py의 키워드 기반 1차 점수를 받음
2. GPT-4o를 호출해 맥락 기반 2차 점수 산출
3. 두 점수를 가중평균으로 융합 (키워드 0.4 + LLM 0.6)
4. 최종 점수, 위험도, 추천 전문분야 반환

설계 원칙:
- LLM 호출 실패 시 키워드 점수만으로 fallback
- LLM은 JSON only 응답 강제
- 위기 감지는 analyzer 결과를 신뢰 (LLM 결과로 덮어쓰지 않음)
- 요약문 생성/NER 마스킹은 본 모듈 책임 아님 (다음 단계)
"""

import json
import logging
from typing import Dict, List

from openai import AsyncOpenAI

from app.core.config import settings
from app.service.analyzer import analyze_messages

logger = logging.getLogger(__name__)
client = AsyncOpenAI(api_key=settings.openai_api_key)

# 융합 가중치
KEYWORD_WEIGHT = 0.4
LLM_WEIGHT = 0.6

# ============================================================
# 1. LLM 점수 산출 프롬프트
# ============================================================

SCORING_SYSTEM_PROMPT = """너는 심리상담 전 사전 점검 대화를 분석하는 평가자야.
사용자의 발화 전체를 읽고, 다음 세 지표를 0~100 정수로 평가해.

평가 지표
- depression: 우울 수준 (무기력, 슬픔, 자기비하, 흥미 상실, 절망감)
- anxiety: 불안 수준 (걱정, 긴장, 공황, 신체화 증상, 회피)
- stress: 스트레스 수준 (압박감, 소진, 짜증, 통제감 상실)

평가 기준
- 0~20: 거의 없음 / 일상 수준
- 21~40: 가벼움
- 41~60: 중등도
- 61~80: 심함
- 81~100: 매우 심함

추가로 다음을 추출해
- recommended_specializations: 추천 상담 전문분야 (최대 3개)
  선택지: ["우울장애", "불안장애", "스트레스/번아웃", "대인관계", "가족관계",
          "직장/학업", "트라우마", "수면문제", "자존감", "정체성"]

출력 형식 (반드시 JSON만, 다른 텍스트 금지)
{
  "depression": <int>,
  "anxiety": <int>,
  "stress": <int>,
  "recommended_specializations": [<string>, ...]
}"""


# ============================================================
# 2. LLM 호출 (실패 시 fallback 가능하도록 예외는 호출자에게 위임)
# ============================================================

async def _llm_score(user_messages: List[str]) -> Dict:
    """
    GPT-4o를 호출해 맥락 기반 점수를 산출.

    Returns:
        {"depression": int, "anxiety": int, "stress": int,
         "recommended_specializations": List[str]}

    Raises:
        Exception: LLM 호출 실패 또는 JSON 파싱 실패 시
    """
    user_text = "\n".join(f"- {msg}" for msg in user_messages)

    response = await client.chat.completions.create(
        model="gpt-4o",
        messages=[
            {"role": "system", "content": SCORING_SYSTEM_PROMPT},
            {"role": "user", "content": f"다음은 내담자의 발화 전체야:\n{user_text}"},
        ],
        max_tokens=300,
        temperature=0.2,  # 일관성 위해 낮게
        response_format={"type": "json_object"},
    )

    raw = response.choices[0].message.content
    parsed = json.loads(raw)

    # 검증 및 클램핑
    return {
        "depression": _clamp(parsed.get("depression", 0)),
        "anxiety": _clamp(parsed.get("anxiety", 0)),
        "stress": _clamp(parsed.get("stress", 0)),
        "recommended_specializations": parsed.get("recommended_specializations", [])[:3],
    }


def _clamp(value, lo: int = 0, hi: int = 100) -> int:
    """정수로 변환 후 0~100 범위로 클램핑."""
    try:
        v = int(value)
    except (TypeError, ValueError):
        return 0
    return max(lo, min(hi, v))


# ============================================================
# 3. 점수 융합
# ============================================================

def _fuse(keyword_score: int, llm_score: int) -> int:
    """키워드 점수와 LLM 점수를 가중평균."""
    fused = keyword_score * KEYWORD_WEIGHT + llm_score * LLM_WEIGHT
    return int(round(fused))


def _recalculate_risk(
    depression: int, anxiety: int, stress: int, is_crisis: bool
) -> str:
    """융합된 점수로 위험도 재계산."""
    if is_crisis:
        return "CRITICAL"
    max_score = max(depression, anxiety, stress)
    if max_score >= 80:
        return "HIGH"
    if max_score >= 60:
        return "MODERATE"
    return "LOW"


# ============================================================
# 4. 메인 진입점
# ============================================================

async def generate_scores(user_messages: List[str]) -> Dict:
    """
    사용자 발화로부터 최종 점수와 메타데이터를 산출.

    파이프라인:
    1. analyzer로 키워드 기반 1차 점수 + 위기 감지
    2. LLM 호출해 맥락 기반 2차 점수
    3. 가중평균 융합 (LLM 실패 시 키워드 점수만 사용)
    4. 위험도 재계산

    Returns:
        {
            "depression_score": int,
            "anxiety_score": int,
            "stress_score": int,
            "risk_level": str,
            "is_crisis": bool,
            "topics": List[str],
            "recommended_specializations": List[str]
        }
    """
    # 1단계: 키워드 분석
    keyword_result = analyze_messages(user_messages)

    # 2단계: LLM 분석 (실패 시 fallback)
    llm_result = None
    try:
        llm_result = await _llm_score(user_messages)
    except Exception as e:
        logger.warning(
            "LLM scoring failed, falling back to keyword-only: %s", e
        )

    # 3단계: 융합
    if llm_result is not None:
        depression = _fuse(keyword_result["depression_score"], llm_result["depression"])
        anxiety = _fuse(keyword_result["anxiety_score"], llm_result["anxiety"])
        stress = _fuse(keyword_result["stress_score"], llm_result["stress"])
        recommended = llm_result["recommended_specializations"]
    else:
        depression = keyword_result["depression_score"]
        anxiety = keyword_result["anxiety_score"]
        stress = keyword_result["stress_score"]
        recommended = []

    # 4단계: 위험도 재계산 (위기 감지는 키워드 결과 신뢰)
    is_crisis = keyword_result["is_crisis"]
    risk_level = _recalculate_risk(depression, anxiety, stress, is_crisis)

    return {
        "depression_score": depression,
        "anxiety_score": anxiety,
        "stress_score": stress,
        "risk_level": risk_level,
        "is_crisis": is_crisis,
        "topics": keyword_result["topics"],
        "recommended_specializations": recommended,
    }