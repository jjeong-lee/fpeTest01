INSERT INTO detail_code (detail_code_id, group_id, code_value, code_name, parent_detail_code_id, display_order, additional_attributes, use_status)
VALUES
  ('00000000-0000-0000-0000-000000000901', 'ACADEMIC_STATUS', 'ENROLLED', '재학', NULL, 1, '{}'::jsonb, 'ACTIVE'),
  ('00000000-0000-0000-0000-000000000902', 'ACADEMIC_STATUS', 'ACTIVE', '재학 중', '00000000-0000-0000-0000-000000000901', 2, '{"linkedCode":"STUDENT_STATUS"}'::jsonb, 'ACTIVE')
ON CONFLICT (detail_code_id) DO NOTHING;
