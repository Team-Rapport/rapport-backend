"""
라포(Rapport) 챗봇 파인튜닝 데이터 생성 스크립트
GPT-4o를 사용해 목표 말투에 맞는 대화 데이터 100개를 생성하고 JSONL로 저장합니다.
"""

import json
import time
import random
from pathlib import Path
from dotenv import load_dotenv
from openai import OpenAI

# rapport-backend/.env 로드 (finetune/ 폴더 기준 한 단계 위)
env_path = Path(__file__).resolve().parent.parent / ".env"
load_dotenv(env_path)
print(f"[DEBUG] .env 경로: {env_path}, 존재: {env_path.exists()}")

client = OpenAI()  # .env의 OPENAI_API_KEY 자동 로드

# ────────────────────────────────────────────────
# 라포 system prompt (실제 서비스와 동일하게 유지)
# ────────────────────────────────────────────────
RAPPO_SYSTEM_PROMPT = """너는 심리상담 전 내담자의 심리 상태 '사전 점검'을 돕는 한국어 챗봇 라포야.

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

# ────────────────────────────────────────────────
# 목표 말투 기준 (few-shot 예시)
# ────────────────────────────────────────────────
TONE_EXAMPLES = """
[목표 말투 예시 — 이 스타일로 생성할 것]

사용자: 요즘 너무 지쳐요
라포: 많이 지치고 힘드시겠어요, 그 마음이 느껴져요. 혹시 지금 가장 힘든 부분이 어떤 건지 이야기해줄 수 있어요?

사용자: 아무것도 하기 싫어요
라포: 아무것도 하기 싫은 기분이 드시는군요. 언제부터 그런 기분이 시작됐는지 기억하세요?

사용자: 잠을 잘 못 자고 있어요
라포: 잠이 잘 안 오면 하루종일 힘들고 피곤하겠어요. 언제부터 잠을 잘 못 주무셨는지 말해줄 수 있어요?

사용자: 직장 때문에 스트레스가 심해요
라포: 직장에서 많은 걸 견디고 계시는군요. 어떤 부분이 특히 힘드세요?

사용자: 이유도 모르겠는데 불안해요
라포: 이유 없이 불안한 마음이 더 답답할 수 있겠네요. 불안감이 언제 가장 심하게 느껴지나요?

[피해야 할 말투]
- "정말 많이 힘드셨겠어요, 제가 옆에서 힘이 되어 드릴게요." → 과장된 감성
- "저도 그 기분 이해해요. 많이 힘드시죠?" → 공감 과잉
- "그럴 때는 밥은 잘 드셨는지, 잠은 잘 주무셨는지 체크해보는 것도 좋아요." → 교훈적
"""

# ────────────────────────────────────────────────
# 다양한 사용자 발화 시나리오
# ────────────────────────────────────────────────
SCENARIOS = [
    # 우울 관련
    "요즘 아무 의욕이 없어요",
    "매일 눈물이 날 것 같아요",
    "예전엔 좋아했던 것들이 이제 재미없어요",
    "혼자 있고 싶어요",
    "내가 왜 사는지 모르겠어요",
    "아무도 나를 이해 못 하는 것 같아요",
    "자꾸 부정적인 생각이 들어요",
    "기분이 가라앉아 있는 날이 많아요",
    "뭘 해도 즐겁지 않아요",
    "미래가 막막하게 느껴져요",
    # 불안 관련
    "자꾸 나쁜 일이 생길 것 같아요",
    "발표 생각만 하면 심장이 두근거려요",
    "실수할까봐 너무 무서워요",
    "사람들 앞에서 말하는 게 너무 떨려요",
    "자꾸 최악의 상황을 상상해요",
    "걱정이 너무 많아서 집중이 안 돼요",
    "뭔가 잘못될 것 같은 느낌이 계속 들어요",
    "가만히 있어도 불안해요",
    "숨이 답답한 느낌이 자주 와요",
    "손발이 자꾸 떨려요",
    # 스트레스/번아웃
    "요즘 너무 바빠서 쉴 틈이 없어요",
    "퇴근하고 나서도 일 생각이 계속 나요",
    "주말에도 피곤해서 아무것도 못 하겠어요",
    "회사에 가기 싫어요",
    "상사가 너무 힘들어요",
    "할 일이 너무 많아서 뭐부터 해야 할지 모르겠어요",
    "몸이 자꾸 아파요",
    "에너지가 하나도 없어요",
    "번아웃이 온 것 같아요",
    "아침에 일어나기가 너무 힘들어요",
    # 대인관계
    "친한 친구랑 사이가 멀어진 것 같아요",
    "가족이랑 자꾸 싸워요",
    "연인이랑 요즘 자꾸 다퉈요",
    "직장 동료랑 트러블이 있어요",
    "사람들이 나를 싫어하는 것 같아요",
    "혼자인 게 너무 외로워요",
    "새로운 환경에 적응이 안 돼요",
    "말을 하면 상처받을까봐 아무 말도 못 하겠어요",
    "누군가한테 화가 나는데 표현을 못 하겠어요",
    "관계가 너무 피곤해요",
    # 수면/신체
    "밤에 잠이 안 와요",
    "새벽에 자꾸 깨요",
    "꿈을 너무 많이 꿔서 피곤해요",
    "자도 자도 피곤해요",
    "식욕이 없어요",
    "너무 많이 먹게 돼요",
    "두통이 자주 와요",
    "몸이 항상 무거워요",
    "집중이 안 돼요",
    "기억력이 떨어진 것 같아요",
    # 자존감/정체성
    "제가 너무 쓸모없는 사람 같아요",
    "뭘 해도 잘 안 되는 것 같아요",
    "자꾸 남들이랑 비교하게 돼요",
    "저만 뒤처지는 것 같아요",
    "제 자신이 싫어요",
    "칭찬을 받아도 기쁘지 않아요",
    "뭐든 다 제 탓인 것 같아요",
    "결정을 못 내리겠어요",
    "항상 제가 부족한 것 같아요",
    "제가 뭘 원하는지 모르겠어요",
    # 상황별 복합
    "취업 준비하는데 너무 힘들어요",
    "졸업하고 나서 방향을 모르겠어요",
    "이직을 고민하고 있는데 불안해요",
    "육아가 너무 지쳐요",
    "부모님 걱정이 너무 돼요",
    "경제적으로 너무 힘들어요",
    "이사하고 나서 적응이 안 돼요",
    "시험이 너무 무서워요",
    "실연당하고 힘들어요",
    "소중한 사람을 잃었어요",
    # 짧고 모호한 발화 (실제 사용자 패턴)
    "그냥 힘들어요",
    "모르겠어요",
    "다 귀찮아요",
    "지쳐요",
    "답답해요",
    "무서워요",
    "외로워요",
    "화가 나요",
    "슬퍼요",
    "멍해요",
    # 복수 증상 복합
    "잠도 못 자고 밥도 못 먹겠어요",
    "불안하고 우울한 것 같아요",
    "집중도 안 되고 아무것도 하기 싫어요",
    "사람들도 만나기 싫고 일도 하기 싫어요",
    "눈물도 나고 화도 나요",
    # 상담에 대한 언급
    "상담받아도 될지 모르겠어요",
    "제가 상담이 필요한 정도인지 모르겠어요",
    "어디서부터 이야기해야 할지 모르겠어요",
    "처음이라 뭘 말해야 할지 몰라요",
    "이런 얘기 해도 되나요",
    # 위기 근접 (감지 연습)
    "살기 싫다는 생각이 들어요",
    "다 사라지고 싶어요",
    "아무도 없으면 좋겠어요",
    "제가 없어져도 아무도 모를 것 같아요",
    "더 이상 버티기 힘들어요",
]

# ────────────────────────────────────────────────
# 데이터 생성 함수
# ────────────────────────────────────────────────
GENERATION_PROMPT = f"""아래 [목표 말투 예시]를 참고해서, 주어진 사용자 발화에 대한 라포의 응답을 생성해줘.

{TONE_EXAMPLES}

규칙:
1. 정확히 2문장: 공감 1문장 + 질문 1문장
2. 과장된 감성 표현 금지 ("제가 옆에서 힘이 되어 드릴게요" 같은 거)
3. 교훈적인 조언 금지
4. 담백하고 자연스러운 존댓말
5. 응답만 출력, 다른 설명 없이

사용자 발화: {{user_message}}
라포 응답:"""


def generate_response(user_message: str, max_retries: int = 3) -> str:
    prompt = GENERATION_PROMPT.replace("{user_message}", user_message)

    for attempt in range(max_retries):
        try:
            response = client.chat.completions.create(
                model="gpt-4o",
                messages=[{"role": "user", "content": prompt}],
                max_tokens=150,
                temperature=0.8,
            )
            return response.choices[0].message.content.strip()
        except Exception as e:
            if attempt < max_retries - 1:
                print(f"  재시도 중... ({attempt + 1}/{max_retries}): {e}")
                time.sleep(2)
            else:
                print(f"  실패: {user_message[:20]}... — {e}")
                return None


def build_jsonl_entry(user_message: str, assistant_response: str) -> dict:
    return {
        "messages": [
            {"role": "system", "content": RAPPO_SYSTEM_PROMPT},
            {"role": "user", "content": user_message},
            {"role": "assistant", "content": assistant_response},
        ]
    }


# ────────────────────────────────────────────────
# 메인 실행
# ────────────────────────────────────────────────
def main():
    output_path = "rapport_finetune_data.jsonl"
    target_count = 100

    # 시나리오 섞기 (재현성을 위해 seed 고정)
    random.seed(42)
    scenarios = SCENARIOS.copy()

    # 100개가 필요하면 시나리오 반복 (현재 95개 → 5개 랜덤 추가)
    while len(scenarios) < target_count:
        scenarios.append(random.choice(SCENARIOS))

    random.shuffle(scenarios)
    scenarios = scenarios[:target_count]

    results = []
    failed = []

    print(f"총 {target_count}개 생성 시작...\n")

    for i, user_msg in enumerate(scenarios, 1):
        print(f"[{i:3d}/{target_count}] {user_msg[:30]}...")

        response = generate_response(user_msg)

        if response:
            entry = build_jsonl_entry(user_msg, response)
            results.append(entry)
            print(f"         → {response[:50]}...")
        else:
            failed.append(user_msg)

        # API rate limit 방지
        time.sleep(0.5)

    # JSONL 저장
    with open(output_path, "w", encoding="utf-8") as f:
        for entry in results:
            f.write(json.dumps(entry, ensure_ascii=False) + "\n")

    print(f"\n✅ 완료: {len(results)}개 저장 → {output_path}")
    if failed:
        print(f"❌ 실패: {len(failed)}개")
        for msg in failed:
            print(f"   - {msg}")


if __name__ == "__main__":
    main()