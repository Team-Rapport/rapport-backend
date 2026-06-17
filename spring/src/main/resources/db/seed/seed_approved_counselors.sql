-- Local seed: approved counselors(10) + session type prices + reviews
-- Safe for existing DB: no delete, no overwrite (insert-if-not-exists).
-- MySQL 8.x

SET NAMES utf8mb4;
SET time_zone = '+09:00';

START TRANSACTION;

-- ============================================================
-- 0) Before counters (for summary)
-- ============================================================
SET @before_users = (
    SELECT COUNT(*) FROM users WHERE email LIKE 'seed.counselor%@rapport.local'
);
SET @before_profiles = (
    SELECT COUNT(*) FROM counselor_profiles cp
    JOIN users u ON u.id = cp.user_id
    WHERE u.email LIKE 'seed.counselor%@rapport.local'
);
SET @before_prices = (
    SELECT COUNT(*) FROM counselor_session_types cst
    JOIN users u ON u.id = cst.counselor_id
    WHERE u.email LIKE 'seed.counselor%@rapport.local'
);
SET @before_reviews = (
    SELECT COUNT(*) FROM reviews
    WHERE content LIKE '[SEED]%'
);
SET @before_schedule_settings = (
    SELECT COUNT(*) FROM counselor_schedule_settings css
    JOIN users u ON u.id = css.counselor_id
    WHERE u.email LIKE 'seed.counselor%@rapport.local'
);
SET @before_schedules = (
    SELECT COUNT(*) FROM counselor_schedules cs
    JOIN users u ON u.id = cs.counselor_id
    WHERE u.email LIKE 'seed.counselor%@rapport.local'
);

-- ============================================================
-- 1) Seed counselor users (10)
-- ============================================================
INSERT INTO users (
    email, password_hash, role, name, gender, birth_date, phone, profile_image_url,
    is_active, is_anonymized, created_at, updated_at, deleted_at
)
SELECT * FROM (
    SELECT 'seed.counselor01@rapport.local' email, NULL password_hash, 'COUNSELOR' role, '김서연' name, 'FEMALE' gender, '1992-03-14' birth_date, '010-7101-1001' phone, 'https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=400' profile_image_url, 1 is_active, 0 is_anonymized, NOW() created_at, NOW() updated_at, NULL deleted_at UNION ALL
    SELECT 'seed.counselor02@rapport.local', NULL, 'COUNSELOR', '박준혁', 'MALE',   '1989-07-08', '010-7101-1002', 'https://images.unsplash.com/photo-1500648767791-00dcc994a43e?w=400', 1, 0, NOW(), NOW(), NULL UNION ALL
    SELECT 'seed.counselor03@rapport.local', NULL, 'COUNSELOR', '이도윤', 'MALE',   '1994-01-22', '010-7101-1003', 'https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=400', 1, 0, NOW(), NOW(), NULL UNION ALL
    SELECT 'seed.counselor04@rapport.local', NULL, 'COUNSELOR', '최하린', 'FEMALE', '1996-11-02', '010-7101-1004', 'https://images.unsplash.com/photo-1544005313-94ddf0286df2?w=400', 1, 0, NOW(), NOW(), NULL UNION ALL
    SELECT 'seed.counselor05@rapport.local', NULL, 'COUNSELOR', '정소희', 'FEMALE', '1987-05-19', '010-7101-1005', 'https://images.unsplash.com/photo-1487412720507-e7ab37603c6f?w=400', 1, 0, NOW(), NOW(), NULL UNION ALL
    SELECT 'seed.counselor06@rapport.local', NULL, 'COUNSELOR', '한지훈', 'MALE',   '1991-09-30', '010-7101-1006', 'https://images.unsplash.com/photo-1506794778202-cad84cf45f1d?w=400', 1, 0, NOW(), NOW(), NULL UNION ALL
    SELECT 'seed.counselor07@rapport.local', NULL, 'COUNSELOR', '윤아름', 'FEMALE', '1986-12-11', '010-7101-1007', 'https://images.unsplash.com/photo-1438761681033-6461ffad8d80?w=400', 1, 0, NOW(), NOW(), NULL UNION ALL
    SELECT 'seed.counselor08@rapport.local', NULL, 'COUNSELOR', '오민재', 'MALE',   '1997-02-27', '010-7101-1008', 'https://images.unsplash.com/photo-1542204625-de293a51f16b?w=400', 1, 0, NOW(), NOW(), NULL UNION ALL
    SELECT 'seed.counselor09@rapport.local', NULL, 'COUNSELOR', '배지은', 'FEMALE', '1993-08-05', '010-7101-1009', 'https://images.unsplash.com/photo-1544723795-3fb6469f5b39?w=400', 1, 0, NOW(), NOW(), NULL UNION ALL
    SELECT 'seed.counselor10@rapport.local', NULL, 'COUNSELOR', '강태원', 'MALE',   '1985-04-18', '010-7101-1010', 'https://images.unsplash.com/photo-1504257432389-52343af06ae3?w=400', 1, 0, NOW(), NOW(), NULL
) s
WHERE NOT EXISTS (
    SELECT 1 FROM users u WHERE u.email = s.email
);

-- ============================================================
-- 2) Seed counselor profiles (APPROVED)
-- ============================================================
INSERT INTO counselor_profiles (
    user_id, license_type, license_number, counselor_gender,
    specializations, approaches, bio, experience_years,
    office_address, approval_status, rejection_reason, approved_at, is_verified,
    average_rating, review_count, created_at, updated_at, deleted_at
)
SELECT u.id,
       x.license_type,
       x.license_number,
       x.counselor_gender,
       CAST(x.specializations AS JSON),
       CAST(x.approaches AS JSON),
       x.bio,
       x.experience_years,
       x.office_address,
       'APPROVED',
       NULL,
       NOW(),
       1,
       NULL,
       0,
       NOW(),
       NOW(),
       NULL
FROM users u
JOIN (
    SELECT 'seed.counselor01@rapport.local' email, '상담심리사 1급' license_type, 'KCP-1-2026-1001' license_number, 'FEMALE' counselor_gender, '["불안","수면","공황"]' specializations, '["인지행동치료(CBT)","호흡/이완 훈련"]' approaches, '수면 문제와 불안 완화를 중심으로 일상 회복을 돕습니다.' bio, 7 experience_years, '서울 강남구 테헤란로 101' office_address UNION ALL
    SELECT 'seed.counselor02@rapport.local', '임상심리사 1급', 'KCP-1-2026-1002', 'MALE',   '["우울","대인관계","자존감"]', '["정서중심치료(EFT)","인지재구성"]', '우울감과 관계 갈등을 함께 정리하며 자기 이해를 높이는 상담을 진행합니다.', 9, '서울 마포구 월드컵북로 77' UNION ALL
    SELECT 'seed.counselor03@rapport.local', '상담심리사 2급', 'KCP-2-2026-1003', 'MALE',   '["직무스트레스","번아웃","완벽주의"]', '["해결중심단기치료(SFBT)","스트레스 코칭"]', '업무 스트레스와 번아웃 관리에 특화된 실천 중심 상담을 제공합니다.', 6, '서울 서초구 서초대로 55' UNION ALL
    SELECT 'seed.counselor04@rapport.local', '청소년상담사 2급', 'YOUTH-2-2026-1004', 'FEMALE', '["연애","이별","애착"]', '["애착기반 상담","대인관계 패턴 분석"]', '관계에서 반복되는 어려움을 함께 이해하고 건강한 경계를 세우도록 돕습니다.', 5, '서울 송파구 올림픽로 222' UNION ALL
    SELECT 'seed.counselor05@rapport.local', '가족상담사', 'FAM-2026-1005', 'FEMALE', '["가족갈등","부부갈등","의사소통"]', '["가족체계치료","비폭력대화(NVC)"]', '가족/부부 관계의 긴장을 완화하고 소통 구조를 재정비하는 상담을 진행합니다.', 10, '서울 영등포구 여의대로 40' UNION ALL
    SELECT 'seed.counselor06@rapport.local', '임상심리사 2급', 'CLIN-2-2026-1006', 'MALE', '["사회불안","발표불안","대인기피"]', '["노출기반 CBT","행동실험"]', '사회적 상황에서의 불안을 단계적으로 완화하는 훈련형 상담을 제공합니다.', 8, '서울 관악구 남부순환로 1811' UNION ALL
    SELECT 'seed.counselor07@rapport.local', '정신건강임상심리사 1급', 'MH-1-2026-1007', 'FEMALE', '["트라우마","PTSD","과각성"]', '["트라우마 인지치료","안정화 기법"]', '안전한 속도로 트라우마 반응을 다루고 일상 기능 회복을 지원합니다.', 11, '서울 종로구 종로 1' UNION ALL
    SELECT 'seed.counselor08@rapport.local', '직업상담사 1급', 'JOB-1-2026-1008', 'MALE', '["진로","취업불안","성취압박"]', '["동기강화면담(MI)","가치기반 의사결정"]', '진로 불안 속에서 현실적인 선택 기준을 세우고 실행 계획을 함께 만듭니다.', 4, '서울 성동구 왕십리로 83' UNION ALL
    SELECT 'seed.counselor09@rapport.local', '상담심리사 2급', 'KCP-2-2026-1009', 'FEMALE', '["감정조절","분노","충동성"]', '["DBT 스킬훈련","마음챙김"]', '강한 감정 기복과 충동 반응을 조절하는 구체적 스킬 중심 상담을 제공합니다.', 7, '서울 노원구 동일로 1414' UNION ALL
    SELECT 'seed.counselor10@rapport.local', '중독전문상담사', 'ADD-2026-1010', 'MALE', '["중독(게임/도박)","회피","무기력"]', '["재발방지모델","행동활성화"]', '중독 행동의 촉발 요인을 파악하고 재발을 줄이는 실천 전략을 설계합니다.', 12, '서울 강서구 공항대로 321'
) x ON x.email = u.email
WHERE NOT EXISTS (
    SELECT 1 FROM counselor_profiles cp WHERE cp.user_id = u.id
);

-- ============================================================
-- 3) Session type price mapping (2 types per counselor)
--    비대면=CALL, 대면=MEETING
-- ============================================================
INSERT INTO counselor_session_types (counselor_id, session_type_id, price)
SELECT u.id, st.id, p.price
FROM users u
JOIN (
    SELECT 'seed.counselor01@rapport.local' email, 'CALL' session_type, 60000 price UNION ALL
    SELECT 'seed.counselor01@rapport.local', 'MEETING', 70000 UNION ALL
    SELECT 'seed.counselor02@rapport.local', 'CALL', 55000 UNION ALL
    SELECT 'seed.counselor02@rapport.local', 'MEETING', 65000 UNION ALL
    SELECT 'seed.counselor03@rapport.local', 'CALL', 50000 UNION ALL
    SELECT 'seed.counselor03@rapport.local', 'MEETING', 60000 UNION ALL
    SELECT 'seed.counselor04@rapport.local', 'CALL', 48000 UNION ALL
    SELECT 'seed.counselor04@rapport.local', 'MEETING', 58000 UNION ALL
    SELECT 'seed.counselor05@rapport.local', 'CALL', 65000 UNION ALL
    SELECT 'seed.counselor05@rapport.local', 'MEETING', 75000 UNION ALL
    SELECT 'seed.counselor06@rapport.local', 'CALL', 53000 UNION ALL
    SELECT 'seed.counselor06@rapport.local', 'MEETING', 63000 UNION ALL
    SELECT 'seed.counselor07@rapport.local', 'CALL', 70000 UNION ALL
    SELECT 'seed.counselor07@rapport.local', 'MEETING', 85000 UNION ALL
    SELECT 'seed.counselor08@rapport.local', 'CALL', 45000 UNION ALL
    SELECT 'seed.counselor08@rapport.local', 'MEETING', 55000 UNION ALL
    SELECT 'seed.counselor09@rapport.local', 'CALL', 52000 UNION ALL
    SELECT 'seed.counselor09@rapport.local', 'MEETING', 62000 UNION ALL
    SELECT 'seed.counselor10@rapport.local', 'CALL', 68000 UNION ALL
    SELECT 'seed.counselor10@rapport.local', 'MEETING', 80000
) p ON p.email = u.email
JOIN session_types st ON st.name = p.session_type
WHERE NOT EXISTS (
    SELECT 1
    FROM counselor_session_types cst
    WHERE cst.counselor_id = u.id
      AND cst.session_type_id = st.id
);

-- ============================================================
-- 4) Schedule settings + available schedules seed
-- ============================================================

-- 4-1) counselor_schedule_settings (slot_unit: 60)
INSERT INTO counselor_schedule_settings (counselor_id, slot_unit, created_at, updated_at)
SELECT u.id, 60, NOW(), NOW()
FROM users u
WHERE u.email LIKE 'seed.counselor%@rapport.local'
  AND NOT EXISTS (
      SELECT 1 FROM counselor_schedule_settings css
      WHERE css.counselor_id = u.id
  );

-- 4-1-a) 일부 상담사는 30분 슬롯으로 설정
-- 요청 반영: seed.counselor01~03 은 slot_unit=30
UPDATE counselor_schedule_settings css
JOIN users u ON u.id = css.counselor_id
SET css.slot_unit = 30,
    css.updated_at = NOW()
WHERE u.email IN (
    'seed.counselor01@rapport.local',
    'seed.counselor02@rapport.local',
    'seed.counselor03@rapport.local'
)
  AND css.slot_unit <> 30;

-- 4-2) counselor_schedules (이번 주 평일 10:00~17:00, 점심 제외)
-- session_type: CALL, MEETING
-- 슬롯: 10-11, 11-12, 13-14, 14-15, 15-16, 16-17 (하루 6개 * 5일 * 2타입 = 60개/상담사)
INSERT INTO counselor_schedules (
    counselor_id, session_type_id, slot_date, start_time, end_time, is_available, created_at, updated_at, version
)
SELECT u.id, st.id, d.slot_date, t.start_time, t.end_time, 1, NOW(), NOW(), 0
FROM users u
JOIN (
    SELECT CURDATE() + INTERVAL (0 - WEEKDAY(CURDATE())) DAY AS slot_date UNION ALL
    SELECT CURDATE() + INTERVAL (1 - WEEKDAY(CURDATE())) DAY UNION ALL
    SELECT CURDATE() + INTERVAL (2 - WEEKDAY(CURDATE())) DAY UNION ALL
    SELECT CURDATE() + INTERVAL (3 - WEEKDAY(CURDATE())) DAY UNION ALL
    SELECT CURDATE() + INTERVAL (4 - WEEKDAY(CURDATE())) DAY
) d
JOIN (
    SELECT '10:00:00' AS start_time, '11:00:00' AS end_time UNION ALL
    SELECT '11:00:00', '12:00:00' UNION ALL
    SELECT '13:00:00', '14:00:00' UNION ALL
    SELECT '14:00:00', '15:00:00' UNION ALL
    SELECT '15:00:00', '16:00:00' UNION ALL
    SELECT '16:00:00', '17:00:00'
) t
JOIN session_types st ON st.name IN ('CALL', 'MEETING')
WHERE u.email LIKE 'seed.counselor%@rapport.local'
  AND NOT EXISTS (
      SELECT 1
      FROM counselor_schedules cs
      WHERE cs.counselor_id = u.id
        AND cs.session_type_id = st.id
        AND cs.slot_date = d.slot_date
        AND cs.start_time = t.start_time
  );

-- 4-2-a) 30분 슬롯 추가 (seed.counselor01~03, CALL 타입)
INSERT INTO counselor_schedules (
    counselor_id, session_type_id, slot_date, start_time, end_time, is_available, created_at, updated_at, version
)
SELECT u.id, st.id, d.slot_date, t.start_time, t.end_time, 1, NOW(), NOW(), 0
FROM users u
JOIN (
    SELECT CURDATE() + INTERVAL (0 - WEEKDAY(CURDATE())) DAY AS slot_date UNION ALL
    SELECT CURDATE() + INTERVAL (1 - WEEKDAY(CURDATE())) DAY UNION ALL
    SELECT CURDATE() + INTERVAL (2 - WEEKDAY(CURDATE())) DAY UNION ALL
    SELECT CURDATE() + INTERVAL (3 - WEEKDAY(CURDATE())) DAY UNION ALL
    SELECT CURDATE() + INTERVAL (4 - WEEKDAY(CURDATE())) DAY
) d
JOIN (
    SELECT '09:00:00' AS start_time, '09:30:00' AS end_time UNION ALL
    SELECT '09:30:00', '10:00:00' UNION ALL
    SELECT '10:00:00', '10:30:00' UNION ALL
    SELECT '10:30:00', '11:00:00' UNION ALL
    SELECT '11:00:00', '11:30:00' UNION ALL
    SELECT '11:30:00', '12:00:00'
) t
JOIN session_types st ON st.name = 'CALL'
WHERE u.email IN (
    'seed.counselor01@rapport.local',
    'seed.counselor02@rapport.local',
    'seed.counselor03@rapport.local'
)
  AND NOT EXISTS (
      SELECT 1
      FROM counselor_schedules cs
      WHERE cs.counselor_id = u.id
        AND cs.session_type_id = st.id
        AND cs.slot_date = d.slot_date
        AND cs.start_time = t.start_time
        AND cs.end_time = t.end_time
  );

-- ============================================================
-- 5) Optional review seed (clients + completed bookings + reviews)
-- ============================================================

-- 4-1) client users (5)
INSERT INTO users (
    email, password_hash, role, name, gender, birth_date, phone, profile_image_url,
    is_active, is_anonymized, created_at, updated_at, deleted_at
)
SELECT * FROM (
    SELECT 'seed.client01@rapport.local' email, NULL password_hash, 'CLIENT' role, '김민지' name, 'FEMALE' gender, '1998-01-03' birth_date, '010-7201-2001' phone, NULL profile_image_url, 1 is_active, 0 is_anonymized, NOW() created_at, NOW() updated_at, NULL deleted_at UNION ALL
    SELECT 'seed.client02@rapport.local', NULL, 'CLIENT', '이현우', 'MALE', '1997-02-10', '010-7201-2002', NULL, 1, 0, NOW(), NOW(), NULL UNION ALL
    SELECT 'seed.client03@rapport.local', NULL, 'CLIENT', '박소연', 'FEMALE', '1999-03-21', '010-7201-2003', NULL, 1, 0, NOW(), NOW(), NULL UNION ALL
    SELECT 'seed.client04@rapport.local', NULL, 'CLIENT', '정지훈', 'MALE', '1996-07-14', '010-7201-2004', NULL, 1, 0, NOW(), NOW(), NULL UNION ALL
    SELECT 'seed.client05@rapport.local', NULL, 'CLIENT', '최유나', 'FEMALE', '2000-09-28', '010-7201-2005', NULL, 1, 0, NOW(), NOW(), NULL
) c
WHERE NOT EXISTS (
    SELECT 1 FROM users u WHERE u.email = c.email
);

-- 4-2) completed bookings for review FK
--      schedule_id is nullable in current migration (V5), so NULL allowed.
INSERT INTO bookings (
    case_id, client_id, counselor_id, schedule_id, report_id, session_type_id,
    concern, status, booked_date, booked_start_time, booked_end_time,
    cancellation_reason, cancelled_by, cancelled_at, created_at, updated_at, deleted_at
)
SELECT NULL, cu.id, co.id, NULL, NULL, st.id,
       '시드 데이터 리뷰용 예약',
       'COMPLETED',
       DATE_SUB(CURDATE(), INTERVAL b.day_offset DAY),
       '10:00:00',
       '10:50:00',
       NULL, NULL, NULL,
       NOW(), NOW(), NULL
FROM (
    SELECT 'seed.counselor01@rapport.local' counselor_email, 'seed.client01@rapport.local' client_email, 15 day_offset, 'CALL' st_name UNION ALL
    SELECT 'seed.counselor01@rapport.local', 'seed.client02@rapport.local', 12, 'MEETING' UNION ALL
    SELECT 'seed.counselor02@rapport.local', 'seed.client03@rapport.local', 14, 'CALL' UNION ALL
    SELECT 'seed.counselor02@rapport.local', 'seed.client04@rapport.local', 9,  'MEETING' UNION ALL
    SELECT 'seed.counselor03@rapport.local', 'seed.client05@rapport.local', 18, 'CALL' UNION ALL
    SELECT 'seed.counselor03@rapport.local', 'seed.client01@rapport.local', 8,  'MEETING' UNION ALL
    SELECT 'seed.counselor04@rapport.local', 'seed.client02@rapport.local', 13, 'CALL' UNION ALL
    SELECT 'seed.counselor04@rapport.local', 'seed.client03@rapport.local', 7,  'MEETING' UNION ALL
    SELECT 'seed.counselor05@rapport.local', 'seed.client04@rapport.local', 16, 'CALL' UNION ALL
    SELECT 'seed.counselor05@rapport.local', 'seed.client05@rapport.local', 6,  'MEETING' UNION ALL
    SELECT 'seed.counselor06@rapport.local', 'seed.client01@rapport.local', 11, 'CALL' UNION ALL
    SELECT 'seed.counselor06@rapport.local', 'seed.client02@rapport.local', 5,  'MEETING' UNION ALL
    SELECT 'seed.counselor07@rapport.local', 'seed.client03@rapport.local', 20, 'CALL' UNION ALL
    SELECT 'seed.counselor07@rapport.local', 'seed.client04@rapport.local', 4,  'MEETING' UNION ALL
    SELECT 'seed.counselor08@rapport.local', 'seed.client05@rapport.local', 19, 'CALL' UNION ALL
    SELECT 'seed.counselor08@rapport.local', 'seed.client01@rapport.local', 3,  'MEETING' UNION ALL
    SELECT 'seed.counselor09@rapport.local', 'seed.client02@rapport.local', 17, 'CALL' UNION ALL
    SELECT 'seed.counselor09@rapport.local', 'seed.client03@rapport.local', 2,  'MEETING' UNION ALL
    SELECT 'seed.counselor10@rapport.local', 'seed.client04@rapport.local', 10, 'CALL' UNION ALL
    SELECT 'seed.counselor10@rapport.local', 'seed.client05@rapport.local', 1,  'MEETING'
) b
JOIN users co ON co.email = b.counselor_email
JOIN users cu ON cu.email = b.client_email
JOIN session_types st ON st.name = b.st_name
WHERE NOT EXISTS (
    SELECT 1
    FROM bookings bk
    WHERE bk.client_id = cu.id
      AND bk.counselor_id = co.id
      AND bk.booked_date = DATE_SUB(CURDATE(), INTERVAL b.day_offset DAY)
      AND bk.booked_start_time = '10:00:00'
);

-- 4-3) reviews (2 per counselor, total 20)
INSERT INTO reviews (booking_id, client_id, counselor_id, rating, content, created_at, updated_at, deleted_at)
SELECT bk.id, bk.client_id, bk.counselor_id, rv.rating, rv.content, NOW(), NOW(), NULL
FROM (
    SELECT 'seed.counselor01@rapport.local' counselor_email, 'seed.client01@rapport.local' client_email, 15 day_offset, 4 rating, '[SEED] 불안 관리에 도움을 많이 받았습니다. 실천 방법이 구체적이었어요.' content UNION ALL
    SELECT 'seed.counselor01@rapport.local', 'seed.client02@rapport.local', 12, 5, '[SEED] 수면 루틴을 정리하는 데 큰 도움이 됐습니다.' UNION ALL
    SELECT 'seed.counselor02@rapport.local', 'seed.client03@rapport.local', 14, 4, '[SEED] 우울감 원인을 차분히 정리할 수 있었습니다.' UNION ALL
    SELECT 'seed.counselor02@rapport.local', 'seed.client04@rapport.local', 9, 5, '[SEED] 관계 갈등을 바라보는 관점이 넓어졌어요.' UNION ALL
    SELECT 'seed.counselor03@rapport.local', 'seed.client05@rapport.local', 18, 4, '[SEED] 번아웃 관리 계획을 세우는 데 효과적이었습니다.' UNION ALL
    SELECT 'seed.counselor03@rapport.local', 'seed.client01@rapport.local', 8, 4, '[SEED] 업무 스트레스 대처법이 실용적이었어요.' UNION ALL
    SELECT 'seed.counselor04@rapport.local', 'seed.client02@rapport.local', 13, 5, '[SEED] 관계 패턴을 이해하는 데 많은 도움이 됐습니다.' UNION ALL
    SELECT 'seed.counselor04@rapport.local', 'seed.client03@rapport.local', 7, 4, '[SEED] 이별 후 감정 정리에 도움을 받았습니다.' UNION ALL
    SELECT 'seed.counselor05@rapport.local', 'seed.client04@rapport.local', 16, 5, '[SEED] 가족과의 대화 방식이 실제로 달라졌어요.' UNION ALL
    SELECT 'seed.counselor05@rapport.local', 'seed.client05@rapport.local', 6, 4, '[SEED] 갈등 상황에서 감정을 조절하는 법을 배웠습니다.' UNION ALL
    SELECT 'seed.counselor06@rapport.local', 'seed.client01@rapport.local', 11, 4, '[SEED] 발표 불안이 줄어드는 과정을 체감했습니다.' UNION ALL
    SELECT 'seed.counselor06@rapport.local', 'seed.client02@rapport.local', 5, 5, '[SEED] 단계별 노출 훈련이 명확해서 좋았습니다.' UNION ALL
    SELECT 'seed.counselor07@rapport.local', 'seed.client03@rapport.local', 20, 5, '[SEED] 트라우마 반응을 안전하게 다루는 느낌이었습니다.' UNION ALL
    SELECT 'seed.counselor07@rapport.local', 'seed.client04@rapport.local', 4, 4, '[SEED] 과각성 완화에 실질적인 도움이 됐습니다.' UNION ALL
    SELECT 'seed.counselor08@rapport.local', 'seed.client05@rapport.local', 19, 4, '[SEED] 진로 선택 기준을 정리하는 데 유익했습니다.' UNION ALL
    SELECT 'seed.counselor08@rapport.local', 'seed.client01@rapport.local', 3, 4, '[SEED] 실행 계획을 세우는 과정이 현실적이었어요.' UNION ALL
    SELECT 'seed.counselor09@rapport.local', 'seed.client02@rapport.local', 17, 5, '[SEED] 감정 기복을 다루는 스킬이 즉시 도움이 됐습니다.' UNION ALL
    SELECT 'seed.counselor09@rapport.local', 'seed.client03@rapport.local', 2, 4, '[SEED] 분노가 올라올 때 적용할 방법이 생겼어요.' UNION ALL
    SELECT 'seed.counselor10@rapport.local', 'seed.client04@rapport.local', 10, 4, '[SEED] 회피 패턴을 인식하고 행동 계획을 만들었습니다.' UNION ALL
    SELECT 'seed.counselor10@rapport.local', 'seed.client05@rapport.local', 1, 5, '[SEED] 재발 방지 전략이 구체적이라 도움이 컸습니다.'
) rv
JOIN users co ON co.email = rv.counselor_email
JOIN users cu ON cu.email = rv.client_email
JOIN bookings bk
  ON bk.counselor_id = co.id
 AND bk.client_id = cu.id
 AND bk.status = 'COMPLETED'
 AND bk.booked_date = DATE_SUB(CURDATE(), INTERVAL rv.day_offset DAY)
 AND bk.booked_start_time = '10:00:00'
WHERE NOT EXISTS (
    SELECT 1 FROM reviews r WHERE r.booking_id = bk.id
);

-- 4-4) average_rating/review_count sync for seeded counselors
UPDATE counselor_profiles cp
JOIN (
    SELECT r.counselor_id,
           ROUND(AVG(r.rating), 2) avg_rating,
           COUNT(*) cnt
    FROM reviews r
    JOIN users u ON u.id = r.counselor_id
    WHERE r.deleted_at IS NULL
      AND u.email LIKE 'seed.counselor%@rapport.local'
    GROUP BY r.counselor_id
) s ON s.counselor_id = cp.user_id
SET cp.average_rating = s.avg_rating,
    cp.review_count = s.cnt,
    cp.updated_at = NOW();

COMMIT;

-- ============================================================
-- 6) Summary (inserted rows in this run)
-- ============================================================
SET @after_users = (
    SELECT COUNT(*) FROM users WHERE email LIKE 'seed.counselor%@rapport.local'
);
SET @after_profiles = (
    SELECT COUNT(*) FROM counselor_profiles cp
    JOIN users u ON u.id = cp.user_id
    WHERE u.email LIKE 'seed.counselor%@rapport.local'
);
SET @after_prices = (
    SELECT COUNT(*) FROM counselor_session_types cst
    JOIN users u ON u.id = cst.counselor_id
    WHERE u.email LIKE 'seed.counselor%@rapport.local'
);
SET @after_reviews = (
    SELECT COUNT(*) FROM reviews
    WHERE content LIKE '[SEED]%'
);
SET @after_schedule_settings = (
    SELECT COUNT(*) FROM counselor_schedule_settings css
    JOIN users u ON u.id = css.counselor_id
    WHERE u.email LIKE 'seed.counselor%@rapport.local'
);
SET @after_schedules = (
    SELECT COUNT(*) FROM counselor_schedules cs
    JOIN users u ON u.id = cs.counselor_id
    WHERE u.email LIKE 'seed.counselor%@rapport.local'
);

SELECT
    (@after_users - @before_users)       AS inserted_users,
    (@after_profiles - @before_profiles) AS inserted_profiles,
    (@after_prices - @before_prices)     AS inserted_session_type_prices,
    (@after_reviews - @before_reviews)   AS inserted_reviews,
    (@after_schedule_settings - @before_schedule_settings) AS inserted_schedule_settings,
    (@after_schedules - @before_schedules) AS inserted_schedules;
