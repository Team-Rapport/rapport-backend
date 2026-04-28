INSERT INTO flyway_schema_history
(installed_rank, version, description, type, script, checksum, installed_by, installed_on, execution_time, success)
SELECT
    (SELECT MAX(installed_rank) FROM flyway_schema_history) + 1,
    '2',
    'add favorites and booking concern',
    'SQL',
    'V2__add_favorites_and_booking_concern.sql',
    0,
    'rapport_user',
    NOW(),
    0,
    1;


