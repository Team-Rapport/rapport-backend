"""
analyzer.py — 키워드/룰 기반 심리 지수 1차 산출

역할:
1. 사용자 발화에서 우울/불안/스트레스 관련 키워드 매칭 → 0~100 점수 산출
2. 토픽 도메인(수면/일/대인관계/건강/금전) 키워드 추출
3. 위기 신호(자살/자해) 감지 — 결정론적 룰로 무조건 잡음

설계 원칙:
- LLM과 독립적으로 동작 (LLM 장애 시 fallback 가능)
- 시스템 프롬프트의 5개 토픽 도메인 / 5개 감정 카테고리와 정렬
- 위기 감지는 절대 LLM에 위임하지 않음 (false negative 위험)
"""

import re
from typing import Dict, List, Tuple

# ============================================================
# 1. 키워드 사전
# ============================================================
# 각 카테고리는 (키워드, 가중치) 튜플 리스트
# 가중치: 1(약함) ~ 3(강함)
# 강한 표현일수록 점수 기여도를 크게

DEPRESSION_KEYWORDS: List[Tuple[str, int]] = [
    # 강도 3 (강한 우울 신호)
    ("죽고 싶", 3), ("사라지고 싶", 3), ("살기 싫", 3),
    ("의미 없", 3), ("희망이 없", 3), ("절망", 3),
    ("아무것도 하기 싫", 3), ("무기력", 3),
    # 강도 2 (중간)
    ("우울", 2), ("슬프", 2), ("눈물", 2), ("울", 2),
    ("외로", 2), ("혼자", 2), ("공허", 2),
    ("자책", 2), ("자존감", 2), ("쓸모없", 2),
    # 강도 1 (약함)
    ("기분이 안 좋", 1), ("처지", 1), ("울적", 1), ("가라앉", 1),
]

ANXIETY_KEYWORDS: List[Tuple[str, int]] = [
    # 강도 3
    ("공황", 3), ("숨이 안", 3), ("숨쉬기 힘", 3),
    ("심장이 빨리", 3), ("두근거", 3), ("가슴이 답답", 3),
    ("불안해서 잠", 3),
    # 강도 2
    ("불안", 2), ("걱정", 2), ("초조", 2), ("긴장", 2),
    ("두렵", 2), ("무섭", 2), ("떨", 2),
    ("안절부절", 2), ("예민", 2),
    # 강도 1
    ("신경 쓰", 1), ("조마조마", 1), ("마음이 안", 1),
]

STRESS_KEYWORDS: List[Tuple[str, int]] = [
    # 강도 3
    ("번아웃", 3), ("탈진", 3), ("한계", 3), ("터질 것 같", 3),
    ("미쳐버리", 3), ("못 견디", 3),
    # 강도 2
    ("스트레스", 2), ("짜증", 2), ("화", 2), ("분노", 2),
    ("힘들", 2), ("지치", 2), ("피곤", 2), ("벅차", 2),
    ("부담", 2), ("압박", 2), ("쫓기", 2),
    # 강도 1
    ("바쁘", 1), ("정신없", 1), ("귀찮", 1),
]

# ============================================================
# 2. 토픽 도메인 키워드 (시스템 프롬프트의 5개 도메인과 정렬)
# ============================================================

TOPIC_KEYWORDS: Dict[str, List[str]] = {
    "수면": [
        "잠", "수면", "불면", "잠들", "깨", "악몽", "선잠",
        "밤새", "새벽", "졸려", "졸음",
    ],
    "일/학업": [
        "회사", "직장", "상사", "동료", "업무", "야근", "출근",
        "학교", "학업", "공부", "시험", "과제", "성적", "취업",
    ],
    "대인관계/가족": [
        "친구", "가족", "부모", "엄마", "아빠", "형", "누나",
        "동생", "남편", "아내", "애인", "연인", "헤어", "이별",
        "갈등", "싸움", "관계",
    ],
    "건강/신체": [
        "몸", "건강", "아프", "통증", "두통", "어지", "소화",
        "식욕", "체중", "살", "병원", "진료",
    ],
    "금전/일상": [
        "돈", "월세", "월급", "생활비", "빚", "대출", "경제",
        "집세", "생계",
    ],
}

# ============================================================
# 3. 위기 감지 패턴 (결정론적 — 절대 LLM에 위임 X)
# ============================================================

CRISIS_PATTERNS: List[str] = [
    # 자살
    r"자살", r"죽고\s*싶", r"죽어버리", r"목숨", r"세상을\s*뜨",
    r"사라지고\s*싶", r"끝내고\s*싶", r"살\s*이유\s*없",
    # 자해
    r"자해", r"칼로\s*긋", r"손목", r"베\s*고\s*싶",
    # 구체적 계획
    r"유서", r"옥상", r"투신", r"약을\s*모", r"목을\s*매",
]

CRISIS_REGEX = re.compile("|".join(CRISIS_PATTERNS))

# ============================================================
# 4. 부정문 감지 (점수 깎기용)
# ============================================================
# "안 우울해요", "불안하지 않아요" 같은 케이스 처리
NEGATION_PATTERNS = [
    r"안\s*\S+",       # "안 우울"
    r"\S+지\s*않",     # "우울하지 않"
    r"별로\s*\S+",     # "별로 안"
    r"괜찮",
]
NEGATION_REGEX = re.compile("|".join(NEGATION_PATTERNS))


# ============================================================
# 5. 점수 산출 함수
# ============================================================

def _score_category(text: str, keywords: List[Tuple[str, int]]) -> int:
    """
    단일 카테고리 점수 산출 (0~100).

    원리:
    - 각 키워드 매칭마다 가중치만큼 가산점
    - 부정문 근처(±10자)에서 매칭되면 절반으로 감산
    - 로그 스케일링으로 0~100에 매핑 (포화 방지)
    """
    raw_score = 0
    for keyword, weight in keywords:
        for match in re.finditer(re.escape(keyword), text):
            start, end = match.span()
            # 매칭 주변 ±10자 컨텍스트 추출 후 부정문 검사
            context = text[max(0, start - 10): end + 10]
            if NEGATION_REGEX.search(context):
                raw_score += weight * 0.3  # 부정문이면 30%만 반영
            else:
                raw_score += weight

    # 로그 스케일링: raw_score 15 → ~75점, 30 → ~90점
    # 0 → 0, 30+ → 100에 점근
    if raw_score <= 0:
        return 0
    import math
    scaled = min(100, int(round(50 * math.log10(raw_score + 1) / math.log10(11) * 1.2)))
    return max(0, min(100, scaled))


def _extract_topics(text: str) -> List[str]:
    """발화에서 등장한 토픽 도메인 추출."""
    found = []
    for topic, words in TOPIC_KEYWORDS.items():
        if any(word in text for word in words):
            found.append(topic)
    return found


def _detect_crisis(text: str) -> bool:
    """위기 신호 감지 — 단 한 번이라도 매칭되면 True."""
    return bool(CRISIS_REGEX.search(text))


def _calculate_risk_level(
    depression: int, anxiety: int, stress: int, is_crisis: bool
) -> str:
    """
    위험도 등급 산출.

    규칙:
    - 위기 감지 시 즉시 CRITICAL
    - 세 점수 중 최댓값 기준
        - 80+ → HIGH
        - 60+ → MODERATE
        - else → LOW
    """
    if is_crisis:
        return "CRITICAL"
    max_score = max(depression, anxiety, stress)
    if max_score >= 80:
        return "HIGH"
    if max_score >= 60:
        return "MODERATE"
    return "LOW"


# ============================================================
# 6. 메인 진입점
# ============================================================

def analyze_messages(user_messages: List[str]) -> Dict:
    """
    사용자 발화 리스트를 받아 1차 분석 결과를 반환.

    Returns:
        {
            "depression_score": int (0~100),
            "anxiety_score": int (0~100),
            "stress_score": int (0~100),
            "topics": List[str],
            "is_crisis": bool,
            "risk_level": str ("LOW" | "MODERATE" | "HIGH" | "CRITICAL")
        }
    """
    if not user_messages:
        return {
            "depression_score": 0,
            "anxiety_score": 0,
            "stress_score": 0,
            "topics": [],
            "is_crisis": False,
            "risk_level": "LOW",
        }

    # 모든 발화를 하나의 텍스트로 합쳐서 분석
    full_text = " ".join(user_messages)

    depression = _score_category(full_text, DEPRESSION_KEYWORDS)
    anxiety = _score_category(full_text, ANXIETY_KEYWORDS)
    stress = _score_category(full_text, STRESS_KEYWORDS)

    topics = _extract_topics(full_text)
    is_crisis = _detect_crisis(full_text)
    risk_level = _calculate_risk_level(depression, anxiety, stress, is_crisis)

    return {
        "depression_score": depression,
        "anxiety_score": anxiety,
        "stress_score": stress,
        "topics": topics,
        "is_crisis": is_crisis,
        "risk_level": risk_level,
    }