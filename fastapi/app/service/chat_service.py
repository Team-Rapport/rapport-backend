from openai import AsyncOpenAI
from app.core.config import settings
from typing import Dict, List

client = AsyncOpenAI(api_key=settings.openai_api_key)

# 세션별 대화 이력 (메모리 — 추후 Redis로 교체 가능)
SESSIONS: Dict[str, List[dict]] = {}

SYSTEM_PROMPT = """너는 심리상담 전 내담자의 심리 상태 '사전 점검'을 돕는 한국어 챗봇 라포야.

목표
- 사용자가 편안하게 현재 상태를 이야기하도록 돕고, 필요한 정보를 한 번에 한 가지씩 묻는다.
- 사용자의 스트레스, 우울, 불안 등 심리 상태를 파악한다.

말투 지침
- 친근하고 따뜻하지만 과장되지 않은 존댓말.
- 교훈적·분석적 설명, 의학적 판단/진단/처방/약물 조언은 금지.
- 반드시 한국어만 사용.

출력 형식
- 정확히 2문장: 공감 1문장 + 구체적 질문 1문장.
- 목록/헤더/코드블록 사용 금지."""

async def create_session(session_id: str):
    SESSIONS[session_id] = []

async def chat(session_id: str, message: str) -> str:
    if session_id not in SESSIONS:
        SESSIONS[session_id] = []

    SESSIONS[session_id].append({"role": "user", "content": message})

    response = await client.chat.completions.create(
        model="gpt-4o-mini",
        messages=[
            {"role": "system", "content": SYSTEM_PROMPT},
            *SESSIONS[session_id]
        ],
        max_tokens=200,
        temperature=0.7,
    )

    reply = response.choices[0].message.content
    SESSIONS[session_id].append({"role": "assistant", "content": reply})
    return reply

async def finalize(session_id: str) -> dict:
    messages = SESSIONS.get(session_id, [])
    user_messages = [m["content"] for m in messages if m["role"] == "user"]

    # 분석은 analyzer.py에 위임
    from app.service.analyzer import analyze_messages
    result = analyze_messages(user_messages)

    # 세션 메모리 정리
    SESSIONS.pop(session_id, None)
    return result