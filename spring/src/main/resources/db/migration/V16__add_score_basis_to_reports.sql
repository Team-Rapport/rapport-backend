ALTER TABLE reports
    ADD COLUMN score_basis JSON NULL COMMENT '점수 산출 근거 (키워드/LLM 점수 및 rationale)' AFTER summary;
