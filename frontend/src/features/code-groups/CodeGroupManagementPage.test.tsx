import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";
import { CodeGroupManagementPage } from "./CodeGroupManagementPage";

const codeGroups = {
  success: true,
  data: [
    {
      groupId: "ACADEMIC_STATUS",
      groupName: "학적 상태",
      description: "학적 상태 구분에 사용하는 공통코드입니다.",
      managingDepartment: "학사지원과",
      useStatus: "ACTIVE",
    },
  ],
  meta: {},
};

describe("CodeGroupManagementPage", () => {
  afterEach(() => vi.restoreAllMocks());

  it("creates a code group, refreshes its management fields, and navigates with the selected group ID", async () => {
    const fetchMock = vi
      .spyOn(globalThis, "fetch")
      .mockResolvedValueOnce(jsonResponse(codeGroups))
      .mockResolvedValueOnce(
        jsonResponse({
          success: true,
          data: {
            groupId: "EVALUATION_TYPE",
            groupName: "평가 유형",
            description: "평가 유형 분류입니다.",
            managingDepartment: "교수지원과",
            useStatus: "ACTIVE",
          },
          meta: {},
        }),
      )
      .mockResolvedValueOnce(
        jsonResponse({
          success: true,
          data: [
            ...codeGroups.data,
            {
              groupId: "EVALUATION_TYPE",
              groupName: "평가 유형",
              description: "평가 유형 분류입니다.",
              managingDepartment: "교수지원과",
              useStatus: "ACTIVE",
            },
          ],
          meta: {},
        }),
      );

    render(<CodeGroupManagementPage />);
    await screen.findByText("학적 상태");
    fireEvent.click(screen.getByRole("button", { name: "코드그룹 등록" }));
    fireEvent.change(screen.getByLabelText("그룹ID"), {
      target: { value: "EVALUATION_TYPE" },
    });
    fireEvent.change(screen.getByLabelText("명칭"), {
      target: { value: "평가 유형" },
    });
    fireEvent.change(screen.getByLabelText("설명"), {
      target: { value: "평가 유형 분류입니다." },
    });
    fireEvent.change(screen.getByLabelText("관리부서"), {
      target: { value: "교수지원과" },
    });
    fireEvent.change(screen.getByLabelText("변경 사유"), {
      target: { value: "코드그룹 등록" },
    });
    fireEvent.click(screen.getByRole("button", { name: "저장" }));

    await screen.findByText("코드그룹이 저장되었습니다.");
    await waitFor(() =>
      expect(fetchMock).toHaveBeenNthCalledWith(
        2,
        "/api/code-groups",
        expect.objectContaining({ method: "POST" }),
      ),
    );
    fireEvent.click(screen.getByRole("button", { name: "평가 유형 선택" }));
    expect(
      screen.getByRole("link", { name: "상세코드 목록" }).getAttribute("href"),
    ).toBe("/system/detail-codes?groupId=EVALUATION_TYPE");
  });

  it("keeps the modal open and shows validation feedback when a required group name is missing", async () => {
    vi.spyOn(globalThis, "fetch").mockResolvedValueOnce(
      jsonResponse(codeGroups),
    );

    render(<CodeGroupManagementPage />);
    await screen.findByText("학적 상태");
    fireEvent.click(screen.getByRole("button", { name: "코드그룹 등록" }));
    fireEvent.change(screen.getByLabelText("그룹ID"), {
      target: { value: "EVALUATION_TYPE" },
    });
    fireEvent.click(screen.getByRole("button", { name: "저장" }));

    expect(screen.getByText("명칭을 입력하세요.")).toBeTruthy();
    expect(screen.getByRole("heading", { name: "코드그룹 등록" })).toBeTruthy();
  });
});

function jsonResponse(body: unknown, status = 200) {
  return new Response(JSON.stringify(body), {
    status,
    headers: { "Content-Type": "application/json" },
  });
}
