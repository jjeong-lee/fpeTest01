import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";
import { UserRoleManagementPage } from "./UserRoleManagementPage";

const userId = "10000000-0000-0000-0000-000000000001";
const roles = {
  success: true,
  data: {
    content: [
      {
        roleCode: "R01",
        assignmentType: "MANUAL",
        effectiveStartDate: "2026-03-01",
        effectiveEndDate: "2026-12-31",
        approverUserId: "00000000-0000-0000-0000-000000000009",
        reason: "초기 역할",
        status: "ACTIVE",
      },
    ],
  },
  meta: {},
};

describe("UserRoleManagementPage", () => {
  afterEach(() => vi.restoreAllMocks());

  it("loads current roles, saves an assignment, and refreshes the current role table", async () => {
    const fetchMock = vi
      .spyOn(globalThis, "fetch")
      .mockResolvedValueOnce(jsonResponse(roles))
      .mockResolvedValueOnce(
        jsonResponse({
          success: true,
          data: {
            roleCode: "R02",
            assignmentType: "MANUAL",
            effectiveStartDate: "2026-09-01",
            effectiveEndDate: "2027-02-28",
            approverUserId: "00000000-0000-0000-0000-000000000009",
            reason: "학과장 보직 부여",
            status: "ACTIVE",
          },
          meta: {},
        }),
      )
      .mockResolvedValueOnce(
        jsonResponse({
          success: true,
          data: {
            content: [
              ...roles.data.content,
              {
                roleCode: "R02",
                assignmentType: "MANUAL",
                effectiveStartDate: "2026-09-01",
                effectiveEndDate: "2027-02-28",
                approverUserId: "00000000-0000-0000-0000-000000000009",
                reason: "학과장 보직 부여",
                status: "ACTIVE",
              },
            ],
          },
          meta: {},
        }),
      );

    render(<UserRoleManagementPage />);
    fireEvent.change(screen.getByLabelText("사용자 식별자"), {
      target: { value: userId },
    });
    fireEvent.click(screen.getByRole("button", { name: "현재 역할 조회" }));

    await screen.findByText("R01");
    fireEvent.change(screen.getByLabelText("역할코드"), {
      target: { value: "R02" },
    });
    fireEvent.change(screen.getByLabelText("승인자"), {
      target: { value: "00000000-0000-0000-0000-000000000009" },
    });
    fireEvent.change(screen.getByLabelText("처리 사유"), {
      target: { value: "학과장 보직 부여" },
    });
    fireEvent.click(screen.getByRole("button", { name: "저장" }));

    await screen.findByText("역할이 저장되었습니다.");
    await waitFor(() =>
      expect(fetchMock).toHaveBeenNthCalledWith(
        2,
        `/api/users/${userId}/roles/R02`,
        expect.objectContaining({ method: "PUT" }),
      ),
    );
    expect(screen.getByText("R02")).toBeTruthy();
  });

  it("confirms revocation before removing the role from the current table", async () => {
    vi.spyOn(globalThis, "fetch")
      .mockResolvedValueOnce(jsonResponse(roles))
      .mockResolvedValueOnce(
        jsonResponse({
          success: true,
          data: { ...roles.data.content[0], status: "REVOKED" },
          meta: {},
        }),
      )
      .mockResolvedValueOnce(
        jsonResponse({ success: true, data: { content: [] }, meta: {} }),
      );

    render(<UserRoleManagementPage />);
    fireEvent.change(screen.getByLabelText("사용자 식별자"), {
      target: { value: userId },
    });
    fireEvent.click(screen.getByRole("button", { name: "현재 역할 조회" }));
    await screen.findByText("R01");
    fireEvent.click(screen.getByRole("button", { name: "R01 회수" }));
    fireEvent.change(screen.getByLabelText("회수 사유"), {
      target: { value: "역할 회수" },
    });
    fireEvent.click(screen.getByRole("button", { name: "회수 확정" }));

    await screen.findByText("역할이 회수되었습니다.");
    expect(screen.getByText("현재 역할이 없습니다.")).toBeTruthy();
  });
});

function jsonResponse(body: unknown, status = 200) {
  return new Response(JSON.stringify(body), {
    status,
    headers: { "Content-Type": "application/json" },
  });
}
