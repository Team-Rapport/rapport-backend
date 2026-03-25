# 라포 (Rapport) — 백엔드

> AI 기반 심리 상담 전 사전 점검 & 상담사 매칭 플랫폼의 백엔드 레포지토리입니다.
> 사용자가 상담사를 만나기 전, AI 챗봇과의 대화를 통해 심리 상태를 사전 점검하고 최적의 상담사를 추천받을 수 있습니다.
> Spring Boot 메인 서버와 FastAPI AI 서버로 구성된 듀얼 서버 아키텍처를 사용합니다.

---

## 기술 스택

![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.5.11-6DB33F?style=flat-square&logo=springboot&logoColor=white)
![Java](https://img.shields.io/badge/Java-21-007396?style=flat-square&logo=openjdk&logoColor=white)
![FastAPI](https://img.shields.io/badge/FastAPI-0.115-009688?style=flat-square&logo=fastapi&logoColor=white)
![Python](https://img.shields.io/badge/Python-3.11-3776AB?style=flat-square&logo=python&logoColor=white)
![MySQL](https://img.shields.io/badge/MySQL-8.0-4479A1?style=flat-square&logo=mysql&logoColor=white)
![Docker](https://img.shields.io/badge/Docker-Compose-2496ED?style=flat-square&logo=docker&logoColor=white)
![AWS](https://img.shields.io/badge/AWS-EC2%20%7C%20RDS%20%7C%20S3-FF9900?style=flat-square&logo=amazonwebservices&logoColor=white)
![GitHub Actions](https://img.shields.io/badge/GitHub_Actions-CI%2FCD-2088FF?style=flat-square&logo=githubactions&logoColor=white)

---

## 디렉토리 구조

```
rapport-backend/
├── spring/                  # Spring Boot 메인 서버 (:8080)
├── fastapi/                 # AI 챗봇 서버 (:8000)
├── docker-compose.yml       # 프로덕션용 (EC2)
├── docker-compose.dev.yml   # 로컬 개발용
├── .env.example             # 환경변수 템플릿
└── .gitignore
```

---

## 시작하기

### 사전 요구사항

| 도구 | 버전 | 비고 |
|---|---|---|
| Java | 21 | [Temurin 다운로드](https://adoptium.net/) |
| Docker Desktop | 최신 | [다운로드](https://www.docker.com/products/docker-desktop/) |
| Python | 3.11 | FastAPI 로컬 개발 시 필요 |
| IntelliJ IDEA | 최신 | Community 또는 Ultimate |

### 1단계 — 레포 클론

```bash
git clone https://github.com/your-org/rapport-backend.git
cd rapport-backend
```

### 2단계 — 환경변수 설정

```bash
cp .env.example .env
```

`.env` 파일을 열어 빈 값을 채웁니다. 실제 값은 **팀장에게 요청**하세요.
각 변수에 대한 설명은 아래 [환경변수 설명표](#환경변수)를 참고하세요.

### 3단계 — Docker로 MySQL + FastAPI 실행

```bash
docker compose -f docker-compose.dev.yml up -d
```

컨테이너가 정상적으로 올라오면 다음 서비스가 실행됩니다.

| 서비스 | 주소 |
|---|---|
| MySQL | `localhost:3306` |
| FastAPI | `http://localhost:8000` |

> **확인 방법**
> ```bash
> docker compose -f docker-compose.dev.yml ps
> ```

### 4단계 — IntelliJ에서 Spring Boot 실행

1. IntelliJ IDEA에서 `spring/` 폴더를 **프로젝트로 열기** (Open → `spring/` 선택)
2. Gradle 동기화가 완료될 때까지 대기
3. `src/main/java/com/rapport/RapportApplication.java` 파일을 열고 ▶ 실행

> **주의**: `.env` 파일의 값이 Spring 환경변수로 자동 주입되지 않을 수 있습니다.
> IntelliJ Run Configuration → `EnvFile` 플러그인을 설치하거나, **Edit Configurations → Environment variables**에 직접 입력하세요.
> 또는 `application.yaml`의 기본값(localhost, rapport_user, rapport1234)으로 우선 실행해볼 수 있습니다.

### 5단계 — 동작 확인

```bash
curl http://localhost:8080/api/health
```

정상 응답 예시:
```json
{"status": "ok"}
```

---

## 환경변수

`.env.example`을 기준으로 한 전체 변수 목록입니다.

| 변수명 | 설명 | 로컬 기본값 |
|---|---|---|
| `SPRING_DATASOURCE_URL` | MySQL 접속 URL | `jdbc:mysql://localhost:3306/rapport` |
| `SPRING_DATASOURCE_USERNAME` | DB 유저명 | `rapport_user` |
| `SPRING_DATASOURCE_PASSWORD` | DB 비밀번호 | `rapport1234` |
| `JWT_SECRET` | JWT 서명 키 (32자 이상) | 임의 문자열 입력 |
| `JWT_EXPIRATION` | JWT 만료 시간 (ms) | `3600000` (1시간) |
| `FRONTEND_ORIGIN` | CORS 허용 프론트 도메인 | `http://localhost:5173` |
| `OPENAI_API_KEY` | OpenAI API 키 | 개인 발급 필요 |
| `AI_SERVER_URL` | FastAPI 서버 주소 | `http://localhost:8000` (EC2: `http://fastapi:8000`) |
| `AWS_ACCESS_KEY_ID` | S3 접근 키 | S3 기능 개발 시 필요 |
| `AWS_SECRET_ACCESS_KEY` | S3 시크릿 키 | S3 기능 개발 시 필요 |
| `AWS_S3_BUCKET` | S3 버킷 이름 | `rapport-uploads-prod` |
| `AWS_REGION` | AWS 리전 | `ap-northeast-2` |

---

## 브랜치 전략

```
main           → 프로덕션 배포 (PR + 1명 승인 필수, 직접 push 금지)
develop        → 개발 통합 브랜치
feat/기능명  → 기능 개발
fix/버그명      → 버그 수정
```

작업 흐름:
1. `develop`에서 `feat/기능명` 브랜치 생성
2. 작업 완료 후 `develop`으로 PR
3. 기능 통합 후 `main`으로 PR → 배포

---

## 커밋 메시지 컨벤션

| 타입 | 용도 |
|---|---|
| `feat` | 새 기능 추가 |
| `fix` | 버그 수정 |
| `chore` | 설정, 패키지 등 기타 변경 |
| `refactor` | 기능 변화 없는 코드 개선 |
| `docs` | 문서 수정 |
| `style` | 포매팅, 세미콜론 등 코드 스타일 |
| `test` | 테스트 코드 추가·수정 |

```
feat: 로그인 JWT 발급 API 구현
fix: 예약 상태 변경 시 권한 체크 누락 수정
chore: Docker Compose 포트 설정 변경
```

---

## 빌드 & 배포

### 로컬 빌드

```bash
cd spring
./gradlew bootJar
```

빌드 결과물: `spring/build/libs/rapport-*.jar`

### 자동 배포 (GitHub Actions)

| 워크플로우 | 트리거 조건 |
|---|---|
| `.github/workflows/deploy-spring.yml` | `main` 브랜치에 `spring/**` 변경 push |
| `.github/workflows/deploy-fastapi.yml` | `main` 브랜치에 `fastapi/**` 변경 push |

배포 과정:
1. JAR 빌드 (Spring) 또는 소스 복사 (FastAPI)
2. SCP로 EC2에 파일 전송
3. EC2에서 Docker 컨테이너 재시작

### 수동 배포

GitHub → **Actions** 탭 → 워크플로우 선택 → **Run workflow** 버튼 클릭

---

## API 문서

Spring Boot 실행 후 Swagger UI에서 API 명세를 확인할 수 있습니다.

- **로컬**: http://localhost:8080/swagger-ui/index.html
- OpenAPI JSON: http://localhost:8080/v3/api-docs
