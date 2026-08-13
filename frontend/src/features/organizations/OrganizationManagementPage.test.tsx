import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";
import { OrganizationManagementPage } from "./OrganizationManagementPage";

const organizations = {
  success: true,
  data: {
    content: [
      {
        organizationCode: "CS",
        organizationName: "컴퓨터교육과",
        organizationType: "DEPARTMENT",
        useStatus: "ACTIVE",
        currentParentOrganizationCode: "COLLEGE",
      },
    ],
    totalElements: 1,
  },
  meta: {},
};

const tree = {
  success: true,
  data: {
    organizationCode: "CS",
    organizationName: "컴퓨터교육과",
    organizationType: "DEPARTMENT",
    useStatus: "ACTIVE",
    parent: {
      organizationCode: "COLLEGE",
      organizationName: "사범대학",
      organizationType: "COLLEGE",
      useStatus: "ACTIVE",
      currentParentOrganizationCode: "KNUE",
    },
    children: [],
    relationHistory: [
      {
        organizationCode: "CS",
        parentOrganizationCode: "COLLEGE",
        effectiveStartDate: "2020-01-01",
        effectiveEndDate: null,
        status: "ACTIVE",
      },
    ],
  },
  meta: {},
};

describe("OrganizationManagementPage", () => {
  afterEach(() => vi.restoreAllMocks());

  it("searches an organization, opens its hierarchy, and refreshes after saving a relation period", async () => {
    const fetchMock = vi
      .spyOn(globalThis, "fetch")
      .mockResolvedValueOnce(jsonResponse(organizations))
      .mockResolvedValueOnce(jsonResponse(tree))
      .mockResolvedValueOnce(
        jsonResponse({
          success: true,
          data: {
            ...tree.data,
            relationHistory: [
              ...tree.data.relationHistory,
              {
                organizationCode: "CS",
                parentOrganizationCode: "GRAD",
                effectiveStartDate: "2099-01-01",
                effectiveEndDate: null,
                status: "ACTIVE",
              },
            ],
          },
          meta: {},
        }),
      )
      .mockResolvedValueOnce(jsonResponse(organizations))
      .mockResolvedValueOnce(
        jsonResponse({ success: true, data: tree.data, meta: {} }),
      );

    render(<OrganizationManagementPage />);
    fireEvent.change(screen.getByLabelText("조직코드"), {
      target: { value: "CS" },
    });
    fireEvent.click(screen.getByRole("button", { name: "조회" }));

    await screen.findByText("컴퓨터교육과");
    fireEvent.click(screen.getByRole("button", { name: "선택" }));
    await screen.findByText("사범대학");

    fireEvent.click(screen.getByRole("button", { name: "관계·적용기간 변경" }));
    fireEvent.change(screen.getByLabelText("상위조직"), {
      target: { value: "GRAD" },
    });
    fireEvent.change(screen.getByLabelText("적용 시작일"), {
      target: { value: "2099-01-01" },
    });
    fireEvent.change(screen.getByLabelText("변경 사유"), {
      target: { value: "조직 개편" },
    });
    fireEvent.click(screen.getByRole("button", { name: "저장" }));

    await screen.findByText("조직 관계가 저장되었습니다.");
    await waitFor(() =>
      expect(fetchMock).toHaveBeenNthCalledWith(
        3,
        "/api/organization-relations/CS",
        expect.objectContaining({ method: "PUT" }),
      ),
    );
    expect(fetchMock.mock.calls[0][0]).toContain("organizationCode=CS");
  });

  it("keeps the modal open when the required effective date is missing", async () => {
    const fetchMock = vi
      .spyOn(globalThis, "fetch")
      .mockResolvedValueOnce(jsonResponse(organizations))
      .mockResolvedValueOnce(jsonResponse(tree));

    render(<OrganizationManagementPage />);
    fireEvent.click(screen.getByRole("button", { name: "조회" }));
    await screen.findByText("컴퓨터교육과");
    fireEvent.click(screen.getByRole("button", { name: "선택" }));
    await screen.findByText("사범대학");
    fireEvent.click(screen.getByRole("button", { name: "관계·적용기간 변경" }));
    fireEvent.change(screen.getByLabelText("변경 사유"), {
      target: { value: "필수값 검증" },
    });
    fireEvent.click(screen.getByRole("button", { name: "저장" }));

    expect(
      screen.getByRole("dialog", { name: "관계·적용기간 변경" }),
    ).toBeTruthy();
    expect(fetchMock).toHaveBeenCalledTimes(2);
  });
});

function jsonResponse(body: unknown, status = 200) {
  return new Response(JSON.stringify(body), {
    status,
    headers: { "Content-Type": "application/json" },
  });
}
