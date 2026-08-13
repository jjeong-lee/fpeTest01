UPDATE menu
SET icon = CASE menu_id
    WHEN '00000000-0000-0000-0000-000000000307' THEN 'settings'
    ELSE COALESCE(icon, 'menu')
  END,
  business_category = COALESCE(business_category, '시스템 관리'),
  description = CASE menu_id
    WHEN '00000000-0000-0000-0000-000000000307' THEN '메뉴 실행정보와 화면 연결을 관리합니다.'
    ELSE COALESCE(description, '시스템 관리 메뉴입니다.')
  END,
  updated_at = CURRENT_TIMESTAMP
WHERE use_status = 'ACTIVE';
