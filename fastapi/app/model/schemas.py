from pydantic import BaseModel
from typing import Optional

# 요청
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

# 응답
class ChatResponse(BaseModel):
    session_id: str
    reply: str

class ReportScore(BaseModel):
    depression: int
    anxiety: int
    stress: int

class ReportResponse(BaseModel):
    session_id: str
    summary: str
    scores: ReportScore
    top_issues: list[str]
    risk_level: int                 # 0 / 60 / 80
    disclaimer: str