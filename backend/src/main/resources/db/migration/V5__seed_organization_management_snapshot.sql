INSERT INTO organization (organization_code, organization_name, organization_type, use_status)
VALUES ('COLLEGE', '사범대학', 'COLLEGE', 'ACTIVE'),
       ('GRAD', '교육대학원', 'GRADUATE_SCHOOL', 'ACTIVE') ON CONFLICT DO NOTHING;

INSERT INTO organization_relation_history (organization_relation_history_id, organization_code, parent_organization_code,
  effective_start_date, status)
VALUES ('20000000-0000-0000-0000-000000000001', 'COLLEGE', 'KNUE', DATE '2020-01-01', 'ACTIVE'),
       ('20000000-0000-0000-0000-000000000002', 'GRAD', 'KNUE', DATE '2020-01-01', 'ACTIVE'),
       ('20000000-0000-0000-0000-000000000003', 'CS', 'COLLEGE', DATE '2020-01-01', 'ACTIVE') ON CONFLICT DO NOTHING;
