import { fireEvent, render, screen } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";
import App from "../App";
import { ApplicationShell, type SessionState } from "./ApplicationShell";

const session: SessionState = {
  status: "authenticated",
  username: "admin",
  allowedMenuPaths: ["/system/users"],
};

describe("ApplicationShell", () => {
  afterEach(() => vi.restoreAllMocks());

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

  it("shows a login form when no active session exists", async () => {
    vi.spyOn(globalThis, "fetch").mockResolvedValue(
      jsonResponse(
        {
          success: false,
          error: {
            code: "UNAUTHENTICATED",
            message: "인증이 필요합니다.",
            fieldErrors: [],
          },
          meta: {},
        },
        401,
      ),
    );

    render(<App />);

    expect(await screen.findByRole("heading", { name: "로그인" })).toBeTruthy();
    expect(screen.getByLabelText("사용자명")).toBeTruthy();
    expect(screen.getByLabelText("비밀번호")).toBeTruthy();
  });

  it("authenticates with the login form and shows the permitted menu", async () => {
    const fetchMock = vi
      .spyOn(globalThis, "fetch")
      .mockResolvedValueOnce(
        jsonResponse(
          {
            success: false,
            error: {
              code: "UNAUTHENTICATED",
              message: "인증이 필요합니다.",
              fieldErrors: [],
            },
            meta: {},
          },
          401,
        ),
      )
      .mockResolvedValueOnce(
        jsonResponse({
          success: true,
          data: {
            username: "admin",
            roles: ["R09"],
            allowedMenuPaths: ["/system/users"],
          },
          meta: {},
        }),
      );

    render(<App />);
    await screen.findByRole("heading", { name: "로그인" });
    fireEvent.change(screen.getByLabelText("사용자명"), {
      target: { value: "admin" },
    });
    fireEvent.change(screen.getByLabelText("비밀번호"), {
      target: { value: "admin" },
    });
    fireEvent.click(screen.getByRole("button", { name: "로그인" }));

    expect(await screen.findByRole("link", { name: "사용자 관리" })).toBeTruthy();
    expect(fetchMock).toHaveBeenNthCalledWith(
      2,
      "/api/auth/login",
      expect.objectContaining({ method: "POST" }),
    );
  });
});

function jsonResponse(body: unknown, status = 200) {
  return new Response(JSON.stringify(body), {
    status,
    headers: { "Content-Type": "application/json" },
  });
}
