import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";
import { MenuStructureManagementPage } from "./MenuStructureManagementPage";

const structure = {
  success: true,
  data: [
    {
      menuId: "00000000-0000-0000-0000-000000000201",
      parentMenuId: null,
      menuName: "시스템 관리",
      displayOrder: 1,
      useStatus: "ACTIVE",
      children: [
        {
          menuId: "00000000-0000-0000-0000-000000000213",
          parentMenuId: "00000000-0000-0000-0000-000000000201",
          menuName: "메뉴 관리",
          displayOrder: 3,
          useStatus: "ACTIVE",
          children: [
            {
              menuId: "00000000-0000-0000-0000-000000000306",
              parentMenuId: "00000000-0000-0000-0000-000000000213",
              menuName: "메뉴 구조 관리",
              displayOrder: 1,
              useStatus: "ACTIVE",
              children: [],
            },
            {
              menuId: "00000000-0000-0000-0000-000000000307",
              parentMenuId: "00000000-0000-0000-0000-000000000213",
              menuName: "메뉴 정보 관리",
              displayOrder: 2,
              useStatus: "ACTIVE",
              children: [],
            },
          ],
        },
      ],
    },
  ],
  meta: {},
};

describe("MenuStructureManagementPage", () => {
  afterEach(() => vi.restoreAllMocks());

  it("loads the menu tree, changes a selected parent, and refreshes the rendered structure", async () => {
    const fetchMock = vi
      .spyOn(globalThis, "fetch")
      .mockResolvedValueOnce(jsonResponse(structure))
      .mockResolvedValueOnce(
        jsonResponse({
          success: true,
          data: {
            ...structure.data[0].children[0].children[1],
            parentMenuId: structure.data[0].menuId,
          },
          meta: {},
        }),
      )
      .mockResolvedValueOnce(jsonResponse(structure));

    render(<MenuStructureManagementPage />);
    await screen.findByRole("button", { name: "메뉴 정보 관리 선택" });
    fireEvent.click(
      screen.getByRole("button", { name: "메뉴 정보 관리 선택" }),
    );
    fireEvent.click(screen.getByRole("button", { name: "부모메뉴 변경" }));
    fireEvent.change(screen.getByLabelText("부모메뉴"), {
      target: { value: structure.data[0].menuId },
    });
    fireEvent.change(screen.getByLabelText("변경 사유"), {
      target: { value: "메뉴 분류 조정" },
    });
    fireEvent.click(screen.getByRole("button", { name: "저장" }));

    await screen.findByText("메뉴 구조가 저장되었습니다.");
    await waitFor(() =>
      expect(fetchMock).toHaveBeenNthCalledWith(
        2,
        "/api/menus/00000000-0000-0000-0000-000000000307/parent",
        expect.objectContaining({ method: "PUT" }),
      ),
    );
  });

  it("keeps the display order modal open when a required order is not provided", async () => {
    vi.spyOn(globalThis, "fetch").mockResolvedValueOnce(
      jsonResponse(structure),
    );

    render(<MenuStructureManagementPage />);
    await screen.findByRole("button", { name: "메뉴 구조 관리 선택" });
    fireEvent.click(
      screen.getByRole("button", { name: "메뉴 구조 관리 선택" }),
    );
    fireEvent.click(screen.getByRole("button", { name: "순서 재정렬" }));
    fireEvent.change(screen.getByLabelText("동일 계층 표시순서"), {
      target: { value: "" },
    });
    fireEvent.change(screen.getByLabelText("변경 사유"), {
      target: { value: "순서 확인" },
    });
    fireEvent.click(screen.getByRole("button", { name: "저장" }));

    expect(
      screen.getByRole("dialog", { name: "표시순서 재정렬" }),
    ).toBeTruthy();
  });
});

function jsonResponse(body: unknown, status = 200) {
  return new Response(JSON.stringify(body), {
    status,
    headers: { "Content-Type": "application/json" },
  });
}
