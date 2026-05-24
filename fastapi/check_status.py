"""
check_status.py
─────────────────────────────────────────────────────────────────────────────
파인튜닝 잡의 상태를 확인하고, succeeded 시 fine_tuned_model ID 를 출력한다.
완료 시 compare_responses.py 의 MODEL_FINETUNED 를 실제 모델 ID 로 자동 교체한다.

Usage:
    python check_status.py <job_id>

Example:
    python check_status.py ftjob-abc123xyz
─────────────────────────────────────────────────────────────────────────────
"""

import os
import re
import sys
from pathlib import Path

from dotenv import load_dotenv
from openai import OpenAI

# ── 경로 설정 ────────────────────────────────────────────────────────────────
FASTAPI_DIR          = Path(__file__).parent          # rapport-backend/fastapi/
ROOT_DIR             = FASTAPI_DIR.parent             # rapport-backend/
ENV_PATH             = ROOT_DIR / ".env"
COMPARE_SCRIPT_PATH  = FASTAPI_DIR / "compare_responses.py"

# ── 환경 변수 로드 ────────────────────────────────────────────────────────────
load_dotenv(ENV_PATH)
OPENAI_API_KEY = os.environ.get("OPENAI_API_KEY")
if not OPENAI_API_KEY:
    print(f"[오류] OPENAI_API_KEY 를 찾을 수 없습니다. ({ENV_PATH})")
    sys.exit(1)

client = OpenAI(api_key=OPENAI_API_KEY)


# ── 상태 레이블 ───────────────────────────────────────────────────────────────
STATUS_EMOJI = {
    "validating_files": "🔍 파일 검증 중",
    "queued":           "⏳ 대기 중",
    "running":          "🔄 학습 중",
    "succeeded":        "✅ 완료",
    "failed":           "❌ 실패",
    "cancelled":        "🚫 취소됨",
}


def patch_compare_responses(model_id: str) -> None:
    """compare_responses.py 의 MODEL_FINETUNED placeholder 를 실제 ID 로 교체한다."""
    if not COMPARE_SCRIPT_PATH.exists():
        print(f"[경고] compare_responses.py 를 찾을 수 없습니다: {COMPARE_SCRIPT_PATH}")
        return

    original = COMPARE_SCRIPT_PATH.read_text(encoding="utf-8")

    # MODEL_FINETUNED = "..." 형태의 줄을 교체 (placeholder 여부 무관)
    pattern = r'(MODEL_FINETUNED\s*=\s*")[^"]+(")([^\n]*)'
    replacement = rf'\g<1>{model_id}\g<2>'
    patched, count = re.subn(pattern, replacement, original)

    if count == 0:
        print("[경고] compare_responses.py 에서 MODEL_FINETUNED 줄을 찾지 못했습니다.")
        return

    COMPARE_SCRIPT_PATH.write_text(patched, encoding="utf-8")
    print(f"[자동 교체] compare_responses.py → MODEL_FINETUNED = \"{model_id}\"")


def check_job(job_id: str) -> None:
    """잡 상태를 조회하고 결과를 출력한다."""
    try:
        job = client.fine_tuning.jobs.retrieve(job_id)
    except Exception as exc:
        print(f"[오류] 잡 조회 실패: {exc}")
        sys.exit(1)

    status_label = STATUS_EMOJI.get(job.status, job.status)

    print()
    print("=" * 60)
    print(f"  잡 ID  : {job.id}")
    print(f"  모델   : {job.model}")
    print(f"  상태   : {status_label}")

    if job.trained_tokens is not None:
        print(f"  학습 토큰: {job.trained_tokens:,}")

    if job.status == "succeeded":
        model_id = job.fine_tuned_model
        print(f"  파인튜닝 모델 ID: {model_id}")
        print("=" * 60)
        print()
        patch_compare_responses(model_id)
        print()
        print("이제 compare_responses.py 를 실행하면 세 모델을 비교할 수 있어요:")
        print("  python compare_responses.py")

    elif job.status == "failed":
        print(f"  오류   : {job.error}")
        print("=" * 60)
        print()
        print("파인튜닝이 실패했습니다. OpenAI 대시보드에서 상세 로그를 확인하세요.")
        print("  https://platform.openai.com/finetune")

    else:
        print("=" * 60)
        print()
        print("아직 완료되지 않았습니다. 잠시 후 다시 확인하세요:")
        print(f"  python check_status.py {job_id}")

    # 최근 이벤트 로그 출력 (최대 5개)
    try:
        events = client.fine_tuning.jobs.list_events(fine_tuning_job_id=job_id, limit=5)
        event_list = list(events)
        if event_list:
            print()
            print("── 최근 이벤트 ──────────────────────────────────────────")
            for event in reversed(event_list):
                print(f"  {event.message}")
    except Exception:
        pass  # 이벤트 조회 실패는 무시


def main() -> None:
    if len(sys.argv) < 2:
        print("Usage: python check_status.py <job_id>")
        print("Example: python check_status.py ftjob-abc123xyz")
        sys.exit(1)

    job_id = sys.argv[1].strip()
    check_job(job_id)


if __name__ == "__main__":
    main()
