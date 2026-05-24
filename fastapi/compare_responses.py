"""
compare_responses.py
─────────────────────────────────────────────────────────────────────────────
동일한 테스트 발화에 대해 두 가지 system prompt의 응답을 나란히 비교한다.

  BEFORE   : 기존 gpt-4o-mini + few-shot 없는 원본 system prompt
  FEW_SHOT : 기존 gpt-4o-mini + few-shot 포함 system prompt

Usage:
    python compare_responses.py

준비물:
    - rapport-backend/.env 에 OPENAI_API_KEY 설정
─────────────────────────────────────────────────────────────────────────────
"""

import importlib.util
import os
import sys
from pathlib import Path
from unittest.mock import MagicMock

# ── 0. 실제 OpenAI 클라이언트 — sys.modules 조작 전에 미리 import ──────────
from openai import OpenAI  # noqa: E402  (반드시 모킹 이전에 위치해야 함)

# ── 1. .env 로드 ────────────────────────────────────────────────────────────
from dotenv import load_dotenv  # noqa: E402

FASTAPI_DIR = Path(__file__).parent
ROOT_DIR    = FASTAPI_DIR.parent          # rapport-backend/
load_dotenv(ROOT_DIR / ".env")

OPENAI_API_KEY = os.environ.get("OPENAI_API_KEY")
if not OPENAI_API_KEY:
    raise RuntimeError(
        "OPENAI_API_KEY를 찾을 수 없습니다. rapport-backend/.env 파일을 확인하세요."
    )

client = OpenAI(api_key=OPENAI_API_KEY)

# ── 2. chat_service 의존성 모킹 ─────────────────────────────────────────────
# chat_service.py 는 Redis·AsyncOpenAI·app.core.config 를 모듈 레벨에서 초기화
# 하므로, 상수(BASE_SYSTEM_PROMPT)만 뽑기 위해 의존성을 먼저 모킹한다.

for _mod in ("redis", "redis.asyncio", "httpx"):
    sys.modules.setdefault(_mod, MagicMock())

# app.core.config 모킹
# ※ openai는 모킹하지 않음 — 위에서 이미 real OpenAI 클라이언트를 import했으므로
#   sys.modules["openai"]를 덮으면 client 내부의 openai.resources 접근이 깨짐
sys.modules["app"] = MagicMock()
sys.modules["app.core"] = MagicMock()
sys.modules["app.core.config"] = MagicMock()

# ── 3. chat_service.py 직접 로드 → BASE_SYSTEM_PROMPT import ────────────────
_CHAT_SERVICE_PATH = FASTAPI_DIR / "app" / "service" / "chat_service.py"
_spec = importlib.util.spec_from_file_location("chat_service", _CHAT_SERVICE_PATH)
_chat_module = importlib.util.module_from_spec(_spec)
_spec.loader.exec_module(_chat_module)  # type: ignore[union-attr]

BASE_SYSTEM_PROMPT: str = _chat_module.BASE_SYSTEM_PROMPT

# ── 4. 두 가지 system prompt 구성 ────────────────────────────────────────────
_FEW_SHOT_HEADER = "═══ 응답 예시 (Few-shot) ═══"

# BEFORE   : few-shot 섹션 이전의 원본 프롬프트
PROMPT_BEFORE: str = BASE_SYSTEM_PROMPT.split(_FEW_SHOT_HEADER)[0].rstrip()

# FEW_SHOT : few-shot 예시까지 포함된 전체 프롬프트
PROMPT_FEW_SHOT: str = BASE_SYSTEM_PROMPT

# ── 5. 모델 ID ────────────────────────────────────────────────────────────────
MODEL = "gpt-4o-mini"

# ── 6. 테스트 발화 목록 ────────────────────────────────────────────────────────
TEST_UTTERANCES = [
    "요즘 잠을 못 자고 있어요",
    "아무것도 하기 싫어요",
    "직장 때문에 너무 힘들어요",
    "살기 싫다는 생각이 들어요",
    "아무도 없으면 좋겠어요",
    "별것도 아닌데 심장이 두근거려요",
]

# ── 7. API 호출 ────────────────────────────────────────────────────────────────
def get_response(system_prompt: str, user_message: str) -> str:
    """단일 발화에 대한 모델 응답을 반환한다. 오류 시 오류 메시지를 반환."""
    try:
        resp = client.chat.completions.create(
            model=MODEL,
            messages=[
                {"role": "system", "content": system_prompt},
                {"role": "user",   "content": user_message},
            ],
            temperature=0.7,
            max_tokens=300,
        )
        return resp.choices[0].message.content.strip()
    except Exception as exc:  # noqa: BLE001
        return f"[오류] {exc}"


# ── 8. 비교 실행 ──────────────────────────────────────────────────────────────
def main() -> None:
    configs = [
        ("BEFORE  ", PROMPT_BEFORE),
        ("FEW_SHOT", PROMPT_FEW_SHOT),
    ]

    divider = "─" * 72

    print("=" * 72)
    print(f"  모델 응답 비교: BEFORE  vs  FEW_SHOT  (model: {MODEL})")
    print("=" * 72)

    for utterance in TEST_UTTERANCES:
        print(f"\n📌 {utterance}")
        for label, prompt in configs:
            response = get_response(prompt, utterance)
            print(f"  {label}: {response}")
        print(divider)

    print("\n완료.")


if __name__ == "__main__":
    main()
