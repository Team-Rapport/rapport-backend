"""
upload_and_finetune.py
─────────────────────────────────────────────────────────────────────────────
rapport_finetune_data.jsonl 을 OpenAI에 업로드하고
gpt-4o-mini-2024-07-18 기반 파인튜닝 잡을 생성한다.

Usage:
    python upload_and_finetune.py

완료 후 출력된 잡 ID 를 check_status.py 에 전달하면 된다.
    python check_status.py <job_id>
─────────────────────────────────────────────────────────────────────────────
"""

import os
import sys
from pathlib import Path

from dotenv import load_dotenv
from openai import OpenAI

# ── 경로 설정 ────────────────────────────────────────────────────────────────
FASTAPI_DIR  = Path(__file__).parent                          # rapport-backend/fastapi/
ROOT_DIR     = FASTAPI_DIR.parent                             # rapport-backend/
ENV_PATH     = ROOT_DIR / ".env"
JSONL_PATH   = ROOT_DIR / "finetune" / "rapport_finetune_data.jsonl"
BASE_MODEL   = "gpt-4o-mini-2024-07-18"

# ── 환경 변수 로드 ────────────────────────────────────────────────────────────
load_dotenv(ENV_PATH)
OPENAI_API_KEY = os.environ.get("OPENAI_API_KEY")
if not OPENAI_API_KEY:
    print(f"[오류] OPENAI_API_KEY 를 찾을 수 없습니다. ({ENV_PATH})")
    sys.exit(1)

client = OpenAI(api_key=OPENAI_API_KEY)

# ── JSONL 존재 확인 ───────────────────────────────────────────────────────────
if not JSONL_PATH.exists():
    print(f"[오류] JSONL 파일을 찾을 수 없습니다: {JSONL_PATH}")
    sys.exit(1)


def upload_file(path: Path) -> str:
    """JSONL 파일을 OpenAI에 업로드하고 file_id 를 반환한다."""
    print(f"[1/2] 파일 업로드 중: {path.name} ({path.stat().st_size / 1024:.1f} KB)")
    with open(path, "rb") as f:
        response = client.files.create(file=f, purpose="fine-tune")
    file_id = response.id
    print(f"      ✅ 업로드 완료 — file_id: {file_id}")
    return file_id


def create_finetune_job(file_id: str) -> str:
    """파인튜닝 잡을 생성하고 job_id 를 반환한다."""
    print(f"[2/2] 파인튜닝 잡 생성 중 (base model: {BASE_MODEL})")
    job = client.fine_tuning.jobs.create(
        training_file=file_id,
        model=BASE_MODEL,
    )
    job_id = job.id
    print(f"      ✅ 잡 생성 완료")
    print()
    print("=" * 60)
    print(f"  잡 ID  : {job_id}")
    print(f"  상태   : {job.status}")
    print("=" * 60)
    print()
    print("상태 확인 명령어:")
    print(f"  python check_status.py {job_id}")
    return job_id


def main() -> None:
    file_id = upload_file(JSONL_PATH)
    create_finetune_job(file_id)


if __name__ == "__main__":
    main()
