import { useEffect, useState } from "react";
import { api, type ApiError } from "./api/client";
import {
  ApplicationShell,
  PermissionDenied,
  type SessionState,
} from "./app/ApplicationShell";
import { LoginPage } from "./app/LoginPage";
import { UserManagementPage } from "./features/users/UserManagementPage";
import { OrganizationManagementPage } from "./features/organizations/OrganizationManagementPage";
import { RoleManagementPage } from "./features/roles/RoleManagementPage";
import { UserRoleManagementPage } from "./features/user-roles/UserRoleManagementPage";
import { MenuPermissionManagementPage } from "./features/menu-permissions/MenuPermissionManagementPage";
import { MenuStructureManagementPage } from "./features/menu-structure/MenuStructureManagementPage";
import { MenuInformationManagementPage } from "./features/menus/MenuInformationManagementPage";
import { CodeGroupManagementPage } from "./features/code-groups/CodeGroupManagementPage";
import { DetailCodeManagementPage } from "./features/detail-codes/DetailCodeManagementPage";

type CurrentUser = {
  username: string;
  roles: string[];
  allowedMenuPaths: string[];
};
type BootstrapState =
  | { state: "loading" }
  | { state: "error" }
  | { state: "unauthenticated" }
  | { state: "denied" }
  | { state: "ready"; session: SessionState };

export default function App() {
  const [bootstrap, setBootstrap] = useState<BootstrapState>({
    state: "loading",
  });

  useEffect(() => {
    api<CurrentUser>("/api/auth/me")
      .then(({ data }) => authenticate(data))
      .catch((error: unknown) => {
        const apiError = error as ApiError;
        setBootstrap(
          apiError.error?.code === "UNAUTHENTICATED"
            ? { state: "unauthenticated" }
            : apiError.error?.code === "MENU_ACCESS_DENIED"
              ? { state: "denied" }
              : { state: "error" },
        );
      });
  }, []);

  function authenticate(user: CurrentUser) {
    const isEntryRoute =
      window.location.pathname === "/" || window.location.pathname === "/login";
    window.history.replaceState(
      {},
      "",
      isEntryRoute
        ? (user.allowedMenuPaths[0] ?? "/")
        : window.location.pathname,
    );
    setBootstrap({
      state: "ready",
      session: {
        status: "authenticated",
        username: user.username,
        allowedMenuPaths: user.allowedMenuPaths,
      },
    });
  }

  async function logout() {
    try {
      await api<void>("/api/auth/logout", { method: "POST" });
    } finally {
      window.history.replaceState({}, "", "/login");
      setBootstrap({ state: "unauthenticated" });
    }
  }

  if (bootstrap.state === "loading")
    return (
      <main className="page-canvas">
        <div className="frame">
          <section className="status-card" aria-live="polite">
            <p className="eyebrow">세션 확인</p>
            <h1>권한 정보를 불러오는 중입니다.</h1>
            <div className="skeleton-line" />
          </section>
        </div>
      </main>
    );

  if (bootstrap.state === "unauthenticated")
    return <LoginPage onAuthenticated={authenticate} />;

  if (bootstrap.state === "denied")
    return (
      <main className="page-canvas">
        <div className="frame">
          <PermissionDenied />
        </div>
      </main>
    );
  if (bootstrap.state === "error")
    return (
      <main className="page-canvas">
        <div className="frame">
          <section className="status-card" role="alert">
            <p className="eyebrow">연결 오류</p>
            <h1>세션 정보를 확인할 수 없습니다.</h1>
            <p>로그인 상태와 네트워크 연결을 확인한 후 다시 시도하세요.</p>
          </section>
        </div>
      </main>
    );

  return (
    <ApplicationShell onLogout={logout} session={bootstrap.session}>
      {window.location.pathname === "/system/users" ? (
        <UserManagementPage />
      ) : window.location.pathname === "/system/organizations" ? (
        <OrganizationManagementPage />
      ) : window.location.pathname === "/system/roles" ? (
        <RoleManagementPage />
      ) : window.location.pathname === "/system/user-roles" ? (
        <UserRoleManagementPage />
      ) : window.location.pathname === "/system/menu-permissions" ? (
        <MenuPermissionManagementPage />
      ) : window.location.pathname === "/system/menu-structure" ? (
        <MenuStructureManagementPage />
      ) : window.location.pathname === "/system/menu-information" ? (
        <MenuInformationManagementPage />
      ) : window.location.pathname === "/system/code-groups" ? (
        <CodeGroupManagementPage />
      ) : window.location.pathname === "/system/detail-codes" ? (
        <DetailCodeManagementPage
          initialGroupId={
            new URLSearchParams(window.location.search).get("groupId") ??
            undefined
          }
        />
      ) : (
        <section className="status-card" role="status">
          <p className="eyebrow">시스템 관리</p>
          <h1>관리할 업무를 선택하세요.</h1>
          <p>
            상단 메뉴에서 권한이 있는 관리 업무를 선택해 계속할 수 있습니다.
          </p>
        </section>
      )}
    </ApplicationShell>
  );
}
