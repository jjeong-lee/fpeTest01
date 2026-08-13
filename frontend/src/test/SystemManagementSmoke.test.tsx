import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import { ApplicationShell, type SessionState } from "../app/ApplicationShell";

const adminSession: SessionState = {
  status: "authenticated",
  username: "admin",
  allowedMenuPaths: [
    "/system/users",
    "/system/organizations",
    "/system/roles",
    "/system/user-roles",
    "/system/menu-permissions",
    "/system/menu-structure",
    "/system/menu-information",
    "/system/code-groups",
    "/system/detail-codes",
  ],
};

const targetMenus = [
  "사용자 관리",
  "조직 관리",
  "역할 관리",
  "사용자 역할 관리",
  "메뉴 권한 관리",
  "메뉴 구조 관리",
  "메뉴 정보 관리",
  "코드그룹 관리",
  "상세코드 관리",
];

describe("system management smoke flow", () => {
  it("shows every phase-one target menu to the authenticated admin", () => {
    render(<ApplicationShell session={adminSession} />);

    for (const menu of targetMenus) {
      expect(screen.getByRole("link", { name: menu })).toBeTruthy();
    }
  });

  it("does not show denied menus and renders a permission state for their direct route", () => {
    render(
      <ApplicationShell
        session={{ ...adminSession, allowedMenuPaths: [] }}
        initialPath="/system/users"
      />,
    );

    for (const menu of targetMenus) {
      expect(screen.queryByRole("link", { name: menu })).toBeNull();
    }
    expect(screen.getByRole("alert").textContent).toContain(
      "접근 권한이 없습니다.",
    );
  });
});
