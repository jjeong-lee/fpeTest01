import type { ReactNode } from "react";

export type SessionState = {
  status: "authenticated";
  username: string;
  allowedMenuPaths: string[];
};

const menuGroups = [
  {
    label: "사용자·조직 관리",
    items: [
      { label: "사용자 관리", path: "/system/users" },
      { label: "조직 관리", path: "/system/organizations" },
    ],
  },
  {
    label: "역할·권한 관리",
    items: [
      { label: "역할 관리", path: "/system/roles" },
      { label: "사용자 역할 관리", path: "/system/user-roles" },
      { label: "메뉴 권한 관리", path: "/system/menu-permissions" },
    ],
  },
  {
    label: "메뉴 관리",
    items: [
      { label: "메뉴 구조 관리", path: "/system/menu-structure" },
      { label: "메뉴 정보 관리", path: "/system/menu-information" },
    ],
  },
  {
    label: "공통코드 관리",
    items: [
      { label: "코드그룹 관리", path: "/system/code-groups" },
      { label: "상세코드 관리", path: "/system/detail-codes" },
    ],
  },
];

type Props = {
  onLogout?: () => void;
  session: SessionState;
  initialPath?: string;
  children?: ReactNode;
};

export function ApplicationShell({
  onLogout,
  session,
  initialPath = window.location.pathname,
  children,
}: Props) {
  const allowedMenuGroups = menuGroups
    .map((group) => ({
      ...group,
      items: group.items.filter((menu) =>
        session.allowedMenuPaths.includes(menu.path),
      ),
    }))
    .filter((group) => group.items.length > 0);
  const hasRoutePermission =
    initialPath === "/" || session.allowedMenuPaths.includes(initialPath);

  return (
    <main className="application-shell">
      <header className="global-header">
        <div className="service-strip">
          <div className="frame">한국교원대학교 · 교수업적평가시스템</div>
        </div>
        <div className="primary-nav frame">
          <a className="brand" href="/">
            KNUE 평가
          </a>
          <nav aria-label="시스템 관리 메뉴" className="menu-nav">
            {allowedMenuGroups.map((group) => (
              <div className="menu-group" key={group.label}>
                <span className="menu-group-label">{group.label}</span>
                <div className="menu-group-links">
                  {group.items.map((menu) => (
                    <a
                      aria-current={
                        initialPath === menu.path ? "page" : undefined
                      }
                      className={
                        initialPath === menu.path ? "is-active" : undefined
                      }
                      key={menu.path}
                      href={menu.path}
                    >
                      {menu.label}
                    </a>
                  ))}
                </div>
              </div>
            ))}
          </nav>
          <div className="account-actions">
            <span className="account-name">{session.username}</span>
            <button className="text-button" onClick={onLogout} type="button">
              로그아웃
            </button>
          </div>
        </div>
      </header>
      <section className="page-canvas">
        <div className="frame">
          {hasRoutePermission ? (
            (children ?? (
              <section className="status-card">
                <p className="eyebrow">시스템 관리</p>
                <h1>공통 관리 화면</h1>
                <p>허용된 메뉴를 선택해 업무를 진행하세요.</p>
              </section>
            ))
          ) : (
            <PermissionDenied />
          )}
        </div>
      </section>
    </main>
  );
}

export function PermissionDenied() {
  return (
    <section className="status-card permission-state" role="alert">
      <p className="eyebrow">권한 확인</p>
      <h1>접근 권한이 없습니다.</h1>
      <p>
        현재 계정에는 이 메뉴를 사용할 권한이 없습니다. 관리자에게 권한을
        요청하세요.
      </p>
    </section>
  );
}
