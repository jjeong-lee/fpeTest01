INSERT INTO code_group (group_id, group_name, description, managing_department, use_status)
VALUES ('ACADEMIC_STATUS', '학적 상태', '학적 상태 구분에 사용하는 공통코드입니다.', '학사지원과', 'ACTIVE')
ON CONFLICT (group_id) DO NOTHING;
