import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";
import { DetailCodeManagementPage } from "./DetailCodeManagementPage";

const groups = {
  success: true,
  data: [
    {
      groupId: "ACADEMIC_STATUS",
      groupName: "학적 상태",
      description: "학적 상태 구분",
      managingDepartment: "학사지원과",
      useStatus: "ACTIVE",
    },
  ],
  meta: {},
};
const detailCodes = {
  success: true,
  data: [
    {
      detailCodeId: "00000000-0000-0000-0000-000000000901",
      groupId: "ACADEMIC_STATUS",
      codeValue: "ENROLLED",
      codeName: "재학",
      parentDetailCodeId: null,
      displayOrder: 1,
      additionalAttributes: {},
      useStatus: "ACTIVE",
    },
    {
      detailCodeId: "00000000-0000-0000-0000-000000000902",
      groupId: "ACADEMIC_STATUS",
      codeValue: "ACTIVE",
      codeName: "재학 중",
      parentDetailCodeId: "00000000-0000-0000-0000-000000000901",
      displayOrder: 2,
      additionalAttributes: { linkedCode: "STUDENT_STATUS" },
      useStatus: "ACTIVE",
    },
  ],
  meta: {},
};

describe("DetailCodeManagementPage", () => {
  afterEach(() => vi.restoreAllMocks());

  it("uses the passed group ID, saves a detail code, and refreshes its hierarchy and attributes", async () => {
    const fetchMock = vi
      .spyOn(globalThis, "fetch")
      .mockResolvedValueOnce(jsonResponse(groups))
      .mockResolvedValueOnce(jsonResponse(detailCodes))
      .mockResolvedValueOnce(
        jsonResponse({
          success: true,
          data: {
            detailCodeId: "00000000-0000-0000-0000-000000000903",
            groupId: "ACADEMIC_STATUS",
            codeValue: "LEAVE",
            codeName: "휴학",
            parentDetailCodeId: "00000000-0000-0000-0000-000000000901",
            displayOrder: 3,
            additionalAttributes: { linkedCode: "LEAVE_TYPE" },
            useStatus: "ACTIVE",
          },
          meta: {},
        }),
      )
      .mockResolvedValueOnce(
        jsonResponse({
          success: true,
          data: [
            ...detailCodes.data,
            {
              detailCodeId: "00000000-0000-0000-0000-000000000903",
              groupId: "ACADEMIC_STATUS",
              codeValue: "LEAVE",
              codeName: "휴학",
              parentDetailCodeId: "00000000-0000-0000-0000-000000000901",
              displayOrder: 3,
              additionalAttributes: { linkedCode: "LEAVE_TYPE" },
              useStatus: "ACTIVE",
            },
          ],
          meta: {},
        }),
      );

    render(<DetailCodeManagementPage initialGroupId="ACADEMIC_STATUS" />);
    await screen.findByText("재학 중");
    fireEvent.click(screen.getByRole("button", { name: "상세코드 등록" }));
    fireEvent.change(screen.getByLabelText("코드값"), {
      target: { value: "LEAVE" },
    });
    fireEvent.change(screen.getByLabelText("코드명"), {
      target: { value: "휴학" },
    });
    fireEvent.change(screen.getByLabelText("상위코드"), {
      target: { value: "00000000-0000-0000-0000-000000000901" },
    });
    fireEvent.change(screen.getByLabelText("정렬순서"), {
      target: { value: "3" },
    });
    fireEvent.change(screen.getByLabelText("추가속성"), {
      target: { value: '{"linkedCode":"LEAVE_TYPE"}' },
    });
    fireEvent.change(screen.getByLabelText("변경 사유"), {
      target: { value: "상세코드 등록" },
    });
    fireEvent.click(screen.getByRole("button", { name: "저장" }));

    await screen.findByText("상세코드가 저장되었습니다.");
    await waitFor(() =>
      expect(fetchMock).toHaveBeenNthCalledWith(
        3,
        "/api/detail-codes",
        expect.objectContaining({ method: "POST" }),
      ),
    );
    expect(screen.getByText("LEAVE_TYPE")).toBeTruthy();
  });

  it("keeps the modal open and shows validation feedback when code name is missing", async () => {
    vi.spyOn(globalThis, "fetch")
      .mockResolvedValueOnce(jsonResponse(groups))
      .mockResolvedValueOnce(jsonResponse(detailCodes));

    render(<DetailCodeManagementPage initialGroupId="ACADEMIC_STATUS" />);
    await screen.findByText("재학 중");
    fireEvent.click(screen.getByRole("button", { name: "상세코드 등록" }));
    fireEvent.change(screen.getByLabelText("코드값"), {
      target: { value: "LEAVE" },
    });
    fireEvent.click(screen.getByRole("button", { name: "저장" }));

    expect(screen.getByText("코드명을 입력하세요.")).toBeTruthy();
    expect(screen.getByRole("heading", { name: "상세코드 등록" })).toBeTruthy();
  });
});

function jsonResponse(body: unknown, status = 200) {
  return new Response(JSON.stringify(body), {
    status,
    headers: { "Content-Type": "application/json" },
  });
}
