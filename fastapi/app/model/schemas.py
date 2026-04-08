from pydantic import BaseModel, Field
from typing import Literal

# ============================================================
# 요청
# ============================================================

class SessionCreateRequest(BaseModel):
    consent: bool
    user_id: int                    # Spring Boot가 JWT 검증 후 전달

class ChatRequest(BaseModel):
    session_id: str
    user_id: int
    message: str

class FinalizeRequest(BaseModel):
    session_id: str
    user_id: int

# ============================================================
# 응답
# ============================================================

class ChatResponse(BaseModel):
    session_id: str
    reply: str
    turn: int
    max_turn: int
    is_final: bool
    is_crisis: bool


class ReportScores(BaseModel):
    """우울/불안/스트레스 0~100 점수."""
    depression: int = Field(..., ge=0, le=100)
    anxiety: int = Field(..., ge=0, le=100)
    stress: int = Field(..., ge=0, le=100)


class ReportResponse(BaseModel):
    """
    AI 사전 점검 리포트 응답.

    DB의 `reports` 테이블 컬럼과 정렬:
    - depression_score / anxiety_score / stress_score → scores
    - risk_level (ENUM)
    - is_crisis_detected → is_crisis
    - keywords → topics (대화에서 추출된 토픽 도메인)
    - recommended_specializations → 상담사 매칭용

    summary 필드는 NER 마스킹 단계에서 추가 예정 (현재 미포함).
    """
    session_id: str
    scores: ReportScores
    risk_level: Literal["LOW", "MODERATE", "HIGH", "CRITICAL"]
    is_crisis: bool
    topics: list[str]
    recommended_specializations: list[str]
    disclaimer: str = (
        "본 결과는 의학적 진단이 아니며, 상담 전 사전 점검 참고용입니다."
    )