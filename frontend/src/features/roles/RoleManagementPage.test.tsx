import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";
import { RoleManagementPage } from "./RoleManagementPage";

const roles = {
  success: true,
  data: {
    content: [
      {
        roleCode: "R01",
        roleName: "교원",
        purpose: "본인 관련 업무를 수행한다.",
        grantCriteria: null,
        dataScopeDefault: null,
        useStatus: "ACTIVE",
      },
      {
        roleCode: "R09",
        roleName: "시스템관리자",
        purpose: "사용자, 조직, 메뉴, 권한, 코드를 관리한다.",
        grantCriteria: "시스템 운영 담당자",
        dataScopeDefault: "전교",
        useStatus: "ACTIVE",
      },
    ],
    totalElements: 2,
  },
  meta: {},
};

describe("RoleManagementPage", () => {
  afterEach(() => vi.restoreAllMocks());

  it("loads the role list when the screen opens", async () => {
    const fetchMock = vi
      .spyOn(globalThis, "fetch")
      .mockResolvedValueOnce(jsonResponse(roles));

    render(<RoleManagementPage />);

    await screen.findByText("시스템관리자");
    expect(fetchMock).toHaveBeenCalledWith("/api/roles", expect.anything());
  });

  it("loads role purposes, updates only the editable policy fields, and refreshes the changed row", async () => {
    const updatedRole = {
      ...roles.data.content[1],
      roleName: "시스템 운영 관리자",
      grantCriteria: "운영 책임자",
      dataScopeDefault: "전체 기관",
    };
    const fetchMock = vi
      .spyOn(globalThis, "fetch")
      .mockResolvedValueOnce(jsonResponse(roles))
      .mockResolvedValueOnce(
        jsonResponse({ success: true, data: updatedRole, meta: {} }),
      )
      .mockResolvedValueOnce(
        jsonResponse({
          success: true,
          data: {
            content: [roles.data.content[0], updatedRole],
            totalElements: 2,
          },
          meta: {},
        }),
      );

    render(<RoleManagementPage />);

    await screen.findByText("시스템관리자");
    expect(
      screen.getByText("사용자, 조직, 메뉴, 권한, 코드를 관리한다."),
    ).toBeTruthy();
    fireEvent.click(screen.getByRole("button", { name: "R09 선택" }));
    fireEvent.change(screen.getByLabelText("역할명"), {
      target: { value: "시스템 운영 관리자" },
    });
    fireEvent.change(screen.getByLabelText("부여 기준"), {
      target: { value: "운영 책임자" },
    });
    fireEvent.change(screen.getByLabelText("데이터 범위 기본값"), {
      target: { value: "전체 기관" },
    });
    fireEvent.change(screen.getByLabelText("변경 사유"), {
      target: { value: "운영 정책 정비" },
    });
    fireEvent.click(screen.getByRole("button", { name: "저장" }));

    await screen.findByText("역할 정책이 저장되었습니다.");
    await waitFor(() =>
      expect(fetchMock).toHaveBeenNthCalledWith(
        2,
        "/api/roles/R09",
        expect.objectContaining({
          method: "PUT",
          body: JSON.stringify({
            roleName: "시스템 운영 관리자",
            grantCriteria: "운영 책임자",
            dataScopeDefault: "전체 기관",
            reason: "운영 정책 정비",
          }),
        }),
      ),
    );
    expect(screen.getAllByText("시스템 운영 관리자").length).toBeGreaterThan(0);
    expect(screen.getAllByText("R09").length).toBeGreaterThan(0);
  });

  it("shows a permission state when the role list is denied", async () => {
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

    render(<RoleManagementPage />);

    await screen.findByText("역할 관리 권한이 없습니다.");
  });
});

function jsonResponse(body: unknown, status = 200) {
  return new Response(JSON.stringify(body), {
    status,
    headers: { "Content-Type": "application/json" },
  });
}
