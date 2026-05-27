SET NAMES utf8mb4;
SET time_zone = '+09:00';

START TRANSACTION;

DROP TEMPORARY TABLE IF EXISTS tmp_counselors;
CREATE TEMPORARY TABLE tmp_counselors (
    id BIGINT UNSIGNED PRIMARY KEY
);

INSERT INTO tmp_counselors (id)
SELECT id FROM users WHERE role = 'COUNSELOR';

-- booking 연관 데이터 먼저 정리
DELETE dm
FROM direct_messages dm
JOIN chat_rooms cr ON cr.id = dm.room_id
JOIN bookings b ON b.id = cr.booking_id
JOIN tmp_counselors tc ON tc.id = b.counselor_id;

DELETE cm
FROM chat_messages cm
JOIN chat_rooms cr ON cr.id = cm.room_id
JOIN bookings b ON b.id = cr.booking_id
JOIN tmp_counselors tc ON tc.id = b.counselor_id;

DELETE sn
FROM session_notes sn
JOIN bookings b ON b.id = sn.session_id
JOIN tmp_counselors tc ON tc.id = b.counselor_id;

DELETE i
FROM intake_forms i
JOIN bookings b ON b.id = i.booking_id
JOIN tmp_counselors tc ON tc.id = b.counselor_id;

DELETE r
FROM reviews r
JOIN bookings b ON b.id = r.booking_id
JOIN tmp_counselors tc ON tc.id = b.counselor_id;

DELETE cr
FROM chat_rooms cr
JOIN bookings b ON b.id = cr.booking_id
JOIN tmp_counselors tc ON tc.id = b.counselor_id;

DELETE b
FROM bookings b
JOIN tmp_counselors tc ON tc.id = b.counselor_id;

-- counselor 직접 연관 데이터 정리
DELETE dm
FROM direct_messages dm
JOIN chat_rooms cr ON cr.id = dm.room_id
JOIN tmp_counselors tc ON tc.id = cr.counselor_id;

DELETE cm
FROM chat_messages cm
JOIN chat_rooms cr ON cr.id = cm.room_id
JOIN tmp_counselors tc ON tc.id = cr.counselor_id;

DELETE cr
FROM chat_rooms cr
JOIN tmp_counselors tc ON tc.id = cr.counselor_id;

DELETE FROM counselor_dayoffs WHERE counselor_id IN (SELECT id FROM tmp_counselors);
DELETE FROM counselor_schedules WHERE counselor_id IN (SELECT id FROM tmp_counselors);
DELETE FROM counselor_schedule_settings WHERE counselor_id IN (SELECT id FROM tmp_counselors);
DELETE FROM counselor_session_types WHERE counselor_id IN (SELECT id FROM tmp_counselors);
DELETE FROM counselor_keywords WHERE counselor_id IN (SELECT id FROM tmp_counselors);
DELETE FROM counselor_credentials WHERE counselor_id IN (SELECT id FROM tmp_counselors);
DELETE FROM counselor_profiles WHERE user_id IN (SELECT id FROM tmp_counselors);
DELETE FROM favorites WHERE counselor_id IN (SELECT id FROM tmp_counselors);
DELETE FROM notifications WHERE user_id IN (SELECT id FROM tmp_counselors);
DELETE FROM refresh_tokens WHERE user_id IN (SELECT id FROM tmp_counselors);
DELETE FROM oauth_accounts WHERE user_id IN (SELECT id FROM tmp_counselors);
DELETE FROM audit_logs WHERE actor_id IN (SELECT id FROM tmp_counselors);
DELETE FROM reports WHERE client_id IN (SELECT id FROM tmp_counselors);
DELETE FROM reviews WHERE counselor_id IN (SELECT id FROM tmp_counselors);
DELETE FROM session_notes WHERE counselor_id IN (SELECT id FROM tmp_counselors);

DELETE FROM users WHERE id IN (SELECT id FROM tmp_counselors);

-- ------------------------------------------------------------------------
-- 새 데모 상담사 10명 생성 (비밀번호: Counselor123!)
-- bcrypt: $2y$10$tBVE6z7mDlNFtfpCQ.unxe9FPlzeLFhqOryNaiYuC8ENa3lhvXitW
-- ------------------------------------------------------------------------
INSERT INTO users (
    email, password_hash, role, name, gender, birth_date, phone, profile_image_url,
    is_active, is_anonymized, created_at, updated_at, deleted_at
)
VALUES
('seed.counselor01@rapport.local','$2y$10$tBVE6z7mDlNFtfpCQ.unxe9FPlzeLFhqOryNaiYuC8ENa3lhvXitW','COUNSELOR','김서연','FEMALE','1992-03-14','010-7101-1001','https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=400',1,0,NOW(),NOW(),NULL),
('seed.counselor02@rapport.local','$2y$10$tBVE6z7mDlNFtfpCQ.unxe9FPlzeLFhqOryNaiYuC8ENa3lhvXitW','COUNSELOR','박준혁','MALE','1989-07-08','010-7101-1002','https://images.unsplash.com/photo-1500648767791-00dcc994a43e?w=400',1,0,NOW(),NOW(),NULL),
('seed.counselor03@rapport.local','$2y$10$tBVE6z7mDlNFtfpCQ.unxe9FPlzeLFhqOryNaiYuC8ENa3lhvXitW','COUNSELOR','이도윤','MALE','1994-01-22','010-7101-1003','https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=400',1,0,NOW(),NOW(),NULL),
('seed.counselor04@rapport.local','$2y$10$tBVE6z7mDlNFtfpCQ.unxe9FPlzeLFhqOryNaiYuC8ENa3lhvXitW','COUNSELOR','최하린','FEMALE','1996-11-02','010-7101-1004','https://images.unsplash.com/photo-1544005313-94ddf0286df2?w=400',1,0,NOW(),NOW(),NULL),
('seed.counselor05@rapport.local','$2y$10$tBVE6z7mDlNFtfpCQ.unxe9FPlzeLFhqOryNaiYuC8ENa3lhvXitW','COUNSELOR','정소희','FEMALE','1987-05-19','010-7101-1005','https://images.unsplash.com/photo-1487412720507-e7ab37603c6f?w=400',1,0,NOW(),NOW(),NULL),
('seed.counselor06@rapport.local','$2y$10$tBVE6z7mDlNFtfpCQ.unxe9FPlzeLFhqOryNaiYuC8ENa3lhvXitW','COUNSELOR','한지훈','MALE','1991-09-30','010-7101-1006','https://images.unsplash.com/photo-1506794778202-cad84cf45f1d?w=400',1,0,NOW(),NOW(),NULL),
('seed.counselor07@rapport.local','$2y$10$tBVE6z7mDlNFtfpCQ.unxe9FPlzeLFhqOryNaiYuC8ENa3lhvXitW','COUNSELOR','윤아름','FEMALE','1986-12-11','010-7101-1007','https://images.unsplash.com/photo-1438761681033-6461ffad8d80?w=400',1,0,NOW(),NOW(),NULL),
('seed.counselor08@rapport.local','$2y$10$tBVE6z7mDlNFtfpCQ.unxe9FPlzeLFhqOryNaiYuC8ENa3lhvXitW','COUNSELOR','오민재','MALE','1997-02-27','010-7101-1008','https://images.unsplash.com/photo-1542204625-de293a51f16b?w=400',1,0,NOW(),NOW(),NULL),
('seed.counselor09@rapport.local','$2y$10$tBVE6z7mDlNFtfpCQ.unxe9FPlzeLFhqOryNaiYuC8ENa3lhvXitW','COUNSELOR','배지은','FEMALE','1993-08-05','010-7101-1009','https://images.unsplash.com/photo-1544723795-3fb6469f5b39?w=400',1,0,NOW(),NOW(),NULL),
('seed.counselor10@rapport.local','$2y$10$tBVE6z7mDlNFtfpCQ.unxe9FPlzeLFhqOryNaiYuC8ENa3lhvXitW','COUNSELOR','강태원','MALE','1985-04-18','010-7101-1010','https://images.unsplash.com/photo-1504257432389-52343af06ae3?w=400',1,0,NOW(),NOW(),NULL);

INSERT INTO counselor_profiles (
    user_id, license_type, license_number, counselor_gender,
    specializations, approaches, symptoms, consultation_modes, bio, experience_years,
    office_address, approval_status, rejection_reason, approved_at, is_verified,
    average_rating, review_count, created_at, updated_at, deleted_at
)
SELECT u.id,
       x.license_type,
       x.license_number,
       x.counselor_gender,
       CAST(x.specializations AS JSON),
       CAST(x.approaches AS JSON),
       CAST(x.symptoms AS JSON),
       CAST(x.consultation_modes AS JSON),
       x.bio,
       x.experience_years,
       x.office_address,
       'APPROVED',
       NULL,
       NOW(),
       1,
       x.average_rating,
       x.review_count,
       NOW(),
       NOW(),
       NULL
FROM users u
JOIN (
    SELECT 'seed.counselor01@rapport.local' email, '상담심리사 1급' license_type, 'KCP-1-2026-1001' license_number, 'FEMALE' counselor_gender, '["불안","수면","공황"]' specializations, '["인지행동치료(CBT)","호흡/이완 훈련"]' approaches, '["우울","불안","불면","공황"]' symptoms, '["FACE_TO_FACE","ONLINE"]' consultation_modes, '수면 문제와 불안 완화를 중심으로 일상 회복을 돕습니다.' bio, 7 experience_years, '서울 강남구 테헤란로 101' office_address, 4.60 average_rating, 12 review_count UNION ALL
    SELECT 'seed.counselor02@rapport.local','임상심리사 1급','KCP-1-2026-1002','MALE','["우울","대인관계","자존감"]','["정서중심치료(EFT)","인지재구성"]','["우울","무기력","불안"]','["FACE_TO_FACE","ONLINE"]','우울감과 관계 갈등을 함께 정리하며 자기 이해를 높이는 상담을 진행합니다.',9,'서울 마포구 월드컵북로 77',4.40,10 UNION ALL
    SELECT 'seed.counselor03@rapport.local','상담심리사 2급','KCP-2-2026-1003','MALE','["직무스트레스","번아웃","완벽주의"]','["해결중심단기치료(SFBT)","스트레스 코칭"]','["번아웃","불면","집중저하"]','["FACE_TO_FACE","ONLINE"]','업무 스트레스와 번아웃 관리에 특화된 실천 중심 상담을 제공합니다.',6,'서울 서초구 서초대로 55',4.30,8 UNION ALL
    SELECT 'seed.counselor04@rapport.local','청소년상담사 2급','YOUTH-2-2026-1004','FEMALE','["연애","이별","애착"]','["애착기반 상담","대인관계 패턴 분석"]','["불안","감정기복","상실감"]','["FACE_TO_FACE","ONLINE"]','관계에서 반복되는 어려움을 함께 이해하고 건강한 경계를 세우도록 돕습니다.',5,'서울 송파구 올림픽로 222',4.20,7 UNION ALL
    SELECT 'seed.counselor05@rapport.local','가족상담사','FAM-2026-1005','FEMALE','["가족갈등","부부갈등","의사소통"]','["가족체계치료","비폭력대화(NVC)"]','["분노","불안","우울"]','["FACE_TO_FACE","ONLINE"]','가족/부부 관계의 긴장을 완화하고 소통 구조를 재정비하는 상담을 진행합니다.',10,'서울 영등포구 여의대로 40',4.80,14 UNION ALL
    SELECT 'seed.counselor06@rapport.local','임상심리사 2급','CLIN-2-2026-1006','MALE','["사회불안","발표불안","대인기피"]','["노출기반 CBT","행동실험"]','["사회불안","회피","긴장"]','["FACE_TO_FACE","ONLINE"]','사회적 상황에서의 불안을 단계적으로 완화하는 훈련형 상담을 제공합니다.',8,'서울 관악구 남부순환로 1811',4.50,11 UNION ALL
    SELECT 'seed.counselor07@rapport.local','정신건강임상심리사 1급','MH-1-2026-1007','FEMALE','["트라우마","PTSD","과각성"]','["트라우마 인지치료","안정화 기법"]','["과각성","악몽","불면"]','["FACE_TO_FACE","ONLINE"]','안전한 속도로 트라우마 반응을 다루고 일상 기능 회복을 지원합니다.',11,'서울 종로구 종로 1',4.90,16 UNION ALL
    SELECT 'seed.counselor08@rapport.local','직업상담사 1급','JOB-1-2026-1008','MALE','["진로","취업불안","성취압박"]','["동기강화면담(MI)","가치기반 의사결정"]','["불안","무기력","자기비난"]','["FACE_TO_FACE","ONLINE"]','진로 불안 속에서 현실적인 선택 기준을 세우고 실행 계획을 함께 만듭니다.',4,'서울 성동구 왕십리로 83',4.10,6 UNION ALL
    SELECT 'seed.counselor09@rapport.local','상담심리사 2급','KCP-2-2026-1009','FEMALE','["감정조절","분노","충동성"]','["DBT 스킬훈련","마음챙김"]','["충동성","분노폭발","후회"]','["FACE_TO_FACE","ONLINE"]','강한 감정 기복과 충동 반응을 조절하는 구체적 스킬 중심 상담을 제공합니다.',7,'서울 노원구 동일로 1414',4.40,9 UNION ALL
    SELECT 'seed.counselor10@rapport.local','중독전문상담사','ADD-2026-1010','MALE','["중독(게임/도박)","회피","무기력"]','["재발방지모델","행동활성화"]','["충동성","무기력","회피"]','["FACE_TO_FACE","ONLINE"]','중독 행동의 촉발 요인을 파악하고 재발을 줄이는 실천 전략을 설계합니다.',12,'서울 강서구 공항대로 321',4.70,13
) x ON x.email = u.email;

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
JOIN session_types st ON st.name = p.session_type;

INSERT INTO counselor_schedule_settings (counselor_id, slot_unit, created_at, updated_at)
SELECT u.id, 60, NOW(), NOW()
FROM users u
WHERE u.email LIKE 'seed.counselor%@rapport.local';

COMMIT;

SELECT
  (SELECT COUNT(*) FROM users WHERE role='COUNSELOR' AND deleted_at IS NULL) AS counselors,
  (SELECT COUNT(*) FROM counselor_profiles) AS profiles,
  (SELECT COUNT(*) FROM counselor_session_types) AS session_type_prices,
  (SELECT COUNT(*) FROM counselor_schedule_settings) AS schedule_settings;
