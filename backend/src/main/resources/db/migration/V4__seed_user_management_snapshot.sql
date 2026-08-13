INSERT INTO organization (organization_code, organization_name, organization_type, use_status)
VALUES ('CS', '컴퓨터교육과', 'DEPARTMENT', 'ACTIVE') ON CONFLICT DO NOTHING;

INSERT INTO korus_personnel_snapshot (employee_no, name, organization_code, position, employment_status, duty, retirement_date, last_synced_at)
VALUES ('20240001', '홍길동', 'CS', '교수', 'ACTIVE', '학과장', DATE '2038-02-28', TIMESTAMP WITH TIME ZONE '2026-08-13 09:00:00+09') ON CONFLICT DO NOTHING;

INSERT INTO user_account (user_id, employee_no, username, password_hash, organization_code, system_enabled, account_status)
VALUES ('10000000-0000-0000-0000-000000000001', '20240001', 'user-20240001', 'not-login-enabled', 'CS', TRUE, 'ACTIVE') ON CONFLICT DO NOTHING;

INSERT INTO user_role (user_role_id, user_id, role_code, assignment_type, approver_user_id, reason, status)
VALUES ('10000000-0000-0000-0000-000000000101', '10000000-0000-0000-0000-000000000001', 'R01', 'MANUAL', '00000000-0000-0000-0000-000000000009', '사용자 관리 검색 시드', 'ACTIVE') ON CONFLICT DO NOTHING;
