import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";
import { MenuPermissionManagementPage } from "./MenuPermissionManagementPage";

const permissions = {
  success: true,
  data: {
    content: [
      {
        subjectType: "ROLE",
        subjectId: "R09",
        menuId: "00000000-0000-0000-0000-000000000305",
        topMenuName: "시스템 관리",
        middleMenuName: "역할·권한 관리",
        menuName: "메뉴 권한 관리",
        accessDecision: "ALLOW",
      },
    ],
    totalElements: 1,
  },
  meta: {},
};

describe("MenuPermissionManagementPage", () => {
  afterEach(() => vi.restoreAllMocks());

  it("loads the selected targets permission matrix, saves a changed decision, and refreshes it", async () => {
    const changed = { ...permissions.data.content[0], accessDecision: "DENY" };
    const fetchMock = vi
      .spyOn(globalThis, "fetch")
      .mockResolvedValueOnce(jsonResponse(permissions))
      .mockResolvedValueOnce(
        jsonResponse({ success: true, data: changed, meta: {} }),
      )
      .mockResolvedValueOnce(
        jsonResponse({
          success: true,
          data: { content: [changed], totalElements: 1 },
          meta: {},
        }),
      );

    render(<MenuPermissionManagementPage />);
    fireEvent.change(screen.getByLabelText("대상 유형"), {
      target: { value: "ROLE" },
    });
    fireEvent.change(screen.getByLabelText("대상 식별자"), {
      target: { value: "R09" },
    });
    fireEvent.click(screen.getByRole("button", { name: "권한 조회" }));

    await screen.findByText("메뉴 권한 관리");
    fireEvent.click(
      screen.getByRole("button", { name: "메뉴 권한 관리 변경" }),
    );
    fireEvent.change(screen.getByLabelText("접근 허용 여부"), {
      target: { value: "DENY" },
    });
    fireEvent.change(screen.getByLabelText("변경 사유"), {
      target: { value: "권한 회수" },
    });
    fireEvent.click(screen.getByRole("button", { name: "저장" }));

    await screen.findByText("메뉴 권한이 저장되었습니다.");
    await waitFor(() =>
      expect(fetchMock).toHaveBeenNthCalledWith(
        2,
        "/api/menu-permissions",
        expect.objectContaining({ method: "PUT" }),
      ),
    );
    expect(screen.getAllByText("차단").length).toBeGreaterThan(0);
  });

  it("keeps the matrix hidden and displays a permission state when the query is denied", async () => {
    vi.spyOn(globalThis, "fetch").mockResolvedValueOnce(
      jsonResponse(
        {
          success: false,
          error: {
            code: "MENU_ACCESS_DENIED",
            message: "권한 없음",
            fieldErrors: [],
          },
          meta: {},
        },
        403,
      ),
    );

    render(<MenuPermissionManagementPage />);
    fireEvent.change(screen.getByLabelText("대상 식별자"), {
      target: { value: "R09" },
    });
    fireEvent.click(screen.getByRole("button", { name: "권한 조회" }));

    await screen.findByText("메뉴 권한 관리 권한이 없습니다.");
    expect(screen.queryByText("메뉴 접근 권한")).toBeNull();
  });
});

function jsonResponse(body: unknown, status = 200) {
  return new Response(JSON.stringify(body), {
    status,
    headers: { "Content-Type": "application/json" },
  });
}
