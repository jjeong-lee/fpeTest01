import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";
import { UserManagementPage } from "./UserManagementPage";

describe("UserManagementPage", () => {
  afterEach(() => vi.restoreAllMocks());

  it("searches, displays readonly KORUS fields, and saves only local system settings", async () => {
    const fetchMock = vi
      .spyOn(globalThis, "fetch")
      .mockResolvedValueOnce(
        jsonResponse({
          success: true,
          data: {
            content: [
              {
                userId: "10000000-0000-0000-0000-000000000001",
                employeeNo: "20240001",
                name: "홍길동",
                organizationCode: "CS",
                position: "교수",
                employmentStatus: "ACTIVE",
                duty: "학과장",
                retirementDate: "2038-02-28",
                lastSyncedAt: "2026-08-13T09:00:00+09:00",
                systemEnabled: true,
                roleCodes: ["R01"],
              },
            ],
            totalElements: 1,
          },
          meta: {},
        }),
      )
      .mockResolvedValueOnce(
        jsonResponse({
          success: true,
          data: { systemEnabled: false, roleCodes: ["R09"] },
          meta: {},
        }),
      )
      .mockResolvedValueOnce(
        jsonResponse({
          success: true,
          data: {
            content: [
              {
                userId: "10000000-0000-0000-0000-000000000001",
                employeeNo: "20240001",
                name: "홍길동",
                organizationCode: "CS",
                position: "교수",
                employmentStatus: "ACTIVE",
                duty: "학과장",
                retirementDate: "2038-02-28",
                lastSyncedAt: "2026-08-13T09:00:00+09:00",
                systemEnabled: false,
                roleCodes: ["R09"],
              },
            ],
            totalElements: 1,
          },
          meta: {},
        }),
      );

    render(<UserManagementPage />);
    fireEvent.change(screen.getByLabelText("교번"), {
      target: { value: "20240001" },
    });
    fireEvent.click(screen.getByRole("button", { name: "조회" }));

    await waitFor(() =>
      expect(screen.getAllByText("홍길동").length).toBeGreaterThan(0),
    );
    fireEvent.click(screen.getByRole("button", { name: "시스템 설정 변경" }));
    expect(
      screen.getByText("KORUS 원천 정보는 변경할 수 없습니다."),
    ).toBeTruthy();
    expect(screen.getAllByText("학과장").length).toBeGreaterThan(0);

    fireEvent.click(screen.getByLabelText("시스템 사용"));
    fireEvent.change(screen.getByLabelText("업무 역할"), {
      target: { value: "R09" },
    });
    fireEvent.change(screen.getByLabelText("변경 사유"), {
      target: { value: "업무 전환" },
    });
    fireEvent.click(screen.getByRole("button", { name: "저장" }));

    await screen.findByText("시스템 설정이 저장되었습니다.");
    await waitFor(() =>
      expect(fetchMock).toHaveBeenNthCalledWith(
        2,
        "/api/users/10000000-0000-0000-0000-000000000001/system-settings",
        expect.objectContaining({ method: "PATCH" }),
      ),
    );
    expect(fetchMock.mock.calls[2][0]).toContain("employeeNo=20240001");
  });

  it("renders an access denied state when the user API returns 403", async () => {
    vi.spyOn(globalThis, "fetch").mockResolvedValue(
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
    render(<UserManagementPage />);
    fireEvent.click(screen.getByRole("button", { name: "조회" }));
    expect(
      await screen.findByText("사용자 관리 권한이 없습니다."),
    ).toBeTruthy();
  });
});

function jsonResponse(body: unknown, status = 200) {
  return new Response(JSON.stringify(body), {
    status,
    headers: { "Content-Type": "application/json" },
  });
}
