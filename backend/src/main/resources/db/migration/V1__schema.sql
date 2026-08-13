CREATE TABLE IF NOT EXISTS user_account (user_id UUID PRIMARY KEY, employee_no VARCHAR(50) NOT NULL UNIQUE, username VARCHAR(100) NOT NULL UNIQUE, password_hash VARCHAR(255) NOT NULL, organization_code VARCHAR(50), system_enabled BOOLEAN NOT NULL DEFAULT TRUE, account_status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE', created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP);
COMMENT ON TABLE user_account IS '내부 시스템 계정과 사용 상태를 관리한다.';
COMMENT ON COLUMN user_account.account_status IS 'ACTIVE:사용중|INACTIVE:사용중지';

CREATE TABLE IF NOT EXISTS organization (organization_code VARCHAR(50) PRIMARY KEY, organization_name VARCHAR(200) NOT NULL, organization_type VARCHAR(30) NOT NULL, use_status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE', created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP);
COMMENT ON TABLE organization IS 'KORUS 조직 Mock snapshot을 조회 전용으로 제공한다.';
COMMENT ON COLUMN organization.organization_type IS 'UNIVERSITY:대학교|GRADUATE_SCHOOL:대학원|COLLEGE:단과대학|DEPARTMENT:학과|OFFICE:부서';
COMMENT ON COLUMN organization.use_status IS 'ACTIVE:사용중|INACTIVE:사용중지';

CREATE TABLE IF NOT EXISTS korus_personnel_snapshot (employee_no VARCHAR(50) PRIMARY KEY, name VARCHAR(100) NOT NULL, organization_code VARCHAR(50) NOT NULL REFERENCES organization(organization_code), position VARCHAR(100), employment_status VARCHAR(20) NOT NULL, duty VARCHAR(100), retirement_date DATE, last_synced_at TIMESTAMP NOT NULL, created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP);
COMMENT ON TABLE korus_personnel_snapshot IS 'KORUS 교직원 Mock snapshot을 조회 전용으로 제공한다.';
COMMENT ON COLUMN korus_personnel_snapshot.employment_status IS 'ACTIVE:재직|RETIRED:퇴직';

CREATE TABLE IF NOT EXISTS organization_relation_history (organization_relation_history_id UUID PRIMARY KEY, organization_code VARCHAR(50) NOT NULL REFERENCES organization(organization_code), parent_organization_code VARCHAR(50) REFERENCES organization(organization_code), effective_start_date DATE NOT NULL, effective_end_date DATE, status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE', created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP);
COMMENT ON TABLE organization_relation_history IS '조직 상위 관계와 적용기간 변경 이력을 보존한다.';
COMMENT ON COLUMN organization_relation_history.status IS 'ACTIVE:유효|INACTIVE:무효';

CREATE TABLE IF NOT EXISTS role (role_code VARCHAR(10) PRIMARY KEY, role_name VARCHAR(100) NOT NULL, purpose VARCHAR(255) NOT NULL, grant_criteria TEXT, data_scope_default TEXT, use_status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE', created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP);
COMMENT ON TABLE role IS '시스템 역할과 역할별 운영 정책을 관리한다.';
COMMENT ON COLUMN role.use_status IS 'ACTIVE:사용중|INACTIVE:사용중지';

CREATE TABLE IF NOT EXISTS user_role (user_role_id UUID PRIMARY KEY, user_id UUID NOT NULL REFERENCES user_account(user_id), role_code VARCHAR(10) NOT NULL REFERENCES role(role_code), assignment_type VARCHAR(30) NOT NULL, effective_start_date DATE, effective_end_date DATE, approver_user_id UUID NOT NULL REFERENCES user_account(user_id), reason TEXT NOT NULL, status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE', created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP);
COMMENT ON TABLE user_role IS '사용자별 역할과 유효기간 및 승인 정보를 관리한다.';
COMMENT ON COLUMN user_role.assignment_type IS 'POSITION_BASED:보직기반|MANUAL:수동';
COMMENT ON COLUMN user_role.status IS 'ACTIVE:유효|REVOKED:회수';

CREATE TABLE IF NOT EXISTS menu (menu_id UUID PRIMARY KEY, parent_menu_id UUID REFERENCES menu(menu_id), menu_name VARCHAR(100) NOT NULL, screen_id VARCHAR(100), url VARCHAR(255), icon VARCHAR(100), business_category VARCHAR(100), description TEXT, display_order INTEGER NOT NULL, use_status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE', created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP);
COMMENT ON TABLE menu IS '시스템 관리 메뉴의 계층과 실행 정보를 관리한다.';
COMMENT ON COLUMN menu.use_status IS 'ACTIVE:사용중|INACTIVE:사용중지';

CREATE TABLE IF NOT EXISTS menu_permission (menu_permission_id UUID PRIMARY KEY, subject_type VARCHAR(20) NOT NULL, subject_id VARCHAR(100) NOT NULL, menu_id UUID NOT NULL REFERENCES menu(menu_id), access_decision VARCHAR(10) NOT NULL, use_status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE', created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, UNIQUE(subject_type, subject_id, menu_id));
COMMENT ON TABLE menu_permission IS '역할, 조직, 사용자별 메뉴 접근 결정을 관리한다.';
COMMENT ON COLUMN menu_permission.subject_type IS 'ROLE:역할|ORGANIZATION:조직|USER:사용자';
COMMENT ON COLUMN menu_permission.access_decision IS 'ALLOW:허용|DENY:차단';
COMMENT ON COLUMN menu_permission.use_status IS 'ACTIVE:사용중|INACTIVE:사용중지';

CREATE TABLE IF NOT EXISTS code_group (group_id VARCHAR(50) PRIMARY KEY, group_name VARCHAR(100) NOT NULL, description TEXT, managing_department VARCHAR(100), use_status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE', created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP);
COMMENT ON TABLE code_group IS '공통코드 그룹을 관리한다.';
COMMENT ON COLUMN code_group.use_status IS 'ACTIVE:사용중|INACTIVE:사용중지';

CREATE TABLE IF NOT EXISTS detail_code (detail_code_id UUID PRIMARY KEY, group_id VARCHAR(50) NOT NULL REFERENCES code_group(group_id), code_value VARCHAR(100) NOT NULL, code_name VARCHAR(100) NOT NULL, parent_detail_code_id UUID REFERENCES detail_code(detail_code_id), display_order INTEGER NOT NULL, additional_attributes JSONB, use_status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE', created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, UNIQUE(group_id, code_value));
COMMENT ON TABLE detail_code IS '코드그룹별 상세코드와 계층 속성을 관리한다.';
COMMENT ON COLUMN detail_code.use_status IS 'ACTIVE:사용중|INACTIVE:사용중지';

CREATE TABLE IF NOT EXISTS session (session_id UUID PRIMARY KEY, user_id UUID NOT NULL REFERENCES user_account(user_id), expires_at TIMESTAMP NOT NULL, status VARCHAR(20) NOT NULL, created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP);
COMMENT ON TABLE session IS '내부 계정의 로그인 세션 상태를 관리한다.';
COMMENT ON COLUMN session.status IS 'ACTIVE:활성|INVALIDATED:무효화|EXPIRED:만료';

CREATE TABLE IF NOT EXISTS change_history (change_history_id UUID PRIMARY KEY, entity_name VARCHAR(100) NOT NULL, entity_id VARCHAR(100) NOT NULL, before_value JSONB, after_value JSONB, actor_user_id UUID NOT NULL REFERENCES user_account(user_id), reason TEXT NOT NULL, processed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP);
COMMENT ON TABLE change_history IS '관리 변경의 전후 값, 처리자, 처리일시, 사유를 내부적으로 보존한다.';

CREATE INDEX IF NOT EXISTS idx_personnel_organization ON korus_personnel_snapshot(organization_code);
CREATE INDEX IF NOT EXISTS idx_relation_organization ON organization_relation_history(organization_code, effective_start_date);
CREATE INDEX IF NOT EXISTS idx_user_role_user ON user_role(user_id, status);
CREATE INDEX IF NOT EXISTS idx_menu_permission_subject ON menu_permission(subject_type, subject_id);
CREATE INDEX IF NOT EXISTS idx_detail_code_group ON detail_code(group_id, display_order);
CREATE INDEX IF NOT EXISTS idx_session_user ON session(user_id, status);
CREATE INDEX IF NOT EXISTS idx_change_history_entity ON change_history(entity_name, entity_id);
