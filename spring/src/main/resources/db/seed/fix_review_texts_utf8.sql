SET NAMES utf8mb4;
SET time_zone = '+09:00';
START TRANSACTION;

UPDATE reviews SET content='불안할 때 바로 적용할 수 있는 방법을 알려주셔서 도움이 많이 됐어요.' WHERE id=32;
UPDATE reviews SET content='상담 분위기가 편안했고 다음 계획을 구체적으로 세울 수 있었습니다.' WHERE id=33;
UPDATE reviews SET content='업무 스트레스 정리에 큰 도움이 되었고 실천 과제도 적절했습니다.' WHERE id=34;
UPDATE reviews SET content='관계 문제를 이해하는 시야가 넓어졌고 마음이 한결 가벼워졌어요.' WHERE id=35;
UPDATE reviews SET content='완벽주의로 힘들었는데 현실적인 루틴을 만들어서 좋았습니다.' WHERE id=36;
UPDATE reviews SET content='경청이 좋았고 상담 목표를 단계적으로 잡아줘서 좋았습니다.' WHERE id=37;
UPDATE reviews SET content='사회불안 때문에 힘들었는데 노출 연습 계획이 큰 도움이 됐습니다.' WHERE id=38;
UPDATE reviews SET content='발표 전 긴장을 다루는 방법을 배워서 실전에서 유용했습니다.' WHERE id=39;
UPDATE reviews SET content='트라우마 반응을 안전하게 다뤄주셔서 신뢰가 생겼습니다.' WHERE id=40;
UPDATE reviews SET content='과각성 완화 훈련이 구체적이라 집에서도 꾸준히 해볼 수 있었어요.' WHERE id=41;
UPDATE reviews SET content='회피 습관을 줄이는 과제가 실용적이었고 동기 부여가 되었습니다.' WHERE id=42;
UPDATE reviews SET content='재발 방지 관점으로 정리해주셔서 방향을 잡는 데 큰 도움이 됐어요.' WHERE id=43;

COMMIT;
