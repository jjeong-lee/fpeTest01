import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import { ApplicationShell, type SessionState } from "./ApplicationShell";

const session: SessionState = {
  status: "authenticated",
  username: "admin",
  allowedMenuPaths: ["/system/users"],
};

describe("ApplicationShell", () => {
  it("hides a menu that the shared permission result does not allow", () => {
    render(<ApplicationShell session={session} />);

    expect(screen.getByRole("link", { name: "사용자 관리" })).toBeTruthy();
    expect(screen.queryByRole("link", { name: "조직 관리" })).toBeNull();
  });

  it("renders a permission-denied state for a direct route without access", () => {
    render(
      <ApplicationShell
        session={{ ...session, allowedMenuPaths: [] }}
        initialPath="/system/users"
      />,
    );

    expect(screen.getByText("접근 권한이 없습니다.")).toBeTruthy();
  });
});
