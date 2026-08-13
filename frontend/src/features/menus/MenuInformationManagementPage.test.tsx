import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";
import { MenuInformationManagementPage } from "./MenuInformationManagementPage";

const menus = {
  success: true,
  data: [
    {
      menuId: "00000000-0000-0000-0000-000000000307",
      menuName: "메뉴 정보 관리",
      screenId: "SCR-MENU-INFORMATION-MANAGEMENT",
      url: "/system/menu-information",
      icon: "settings",
      businessCategory: "시스템 관리",
      description: "메뉴 실행정보와 화면 연결을 관리합니다.",
      useStatus: "ACTIVE",
    },
  ],
  meta: {},
};

describe("MenuInformationManagementPage", () => {
  afterEach(() => vi.restoreAllMocks());

  it("loads execution information, saves the selected menu, and refreshes the changed screen connection", async () => {
    const fetchMock = vi
      .spyOn(globalThis, "fetch")
      .mockResolvedValueOnce(jsonResponse(menus))
      .mockResolvedValueOnce(
        jsonResponse({
          success: true,
          data: {
            ...menus.data[0],
            menuName: "메뉴 실행정보 관리",
            screenId: "SCR-MENU-EXECUTION-MANAGEMENT",
            url: "/system/menu-information/execution",
          },
          meta: {},
        }),
      )
      .mockResolvedValueOnce(
        jsonResponse({
          success: true,
          data: [
            {
              ...menus.data[0],
              menuName: "메뉴 실행정보 관리",
              screenId: "SCR-MENU-EXECUTION-MANAGEMENT",
              url: "/system/menu-information/execution",
            },
          ],
          meta: {},
        }),
      );

    render(<MenuInformationManagementPage />);
    await screen.findByText("메뉴 정보 관리");
    fireEvent.click(
      screen.getByRole("button", { name: "메뉴 정보 관리 실행정보 편집" }),
    );
    fireEvent.change(screen.getByLabelText("메뉴명"), {
      target: { value: "메뉴 실행정보 관리" },
    });
    fireEvent.change(screen.getByLabelText("화면ID"), {
      target: { value: "SCR-MENU-EXECUTION-MANAGEMENT" },
    });
    fireEvent.change(screen.getByLabelText("URL"), {
      target: { value: "/system/menu-information/execution" },
    });
    fireEvent.change(screen.getByLabelText("변경 사유"), {
      target: { value: "화면 연결 갱신" },
    });
    fireEvent.click(screen.getByRole("button", { name: "저장" }));

    await screen.findByText("메뉴 실행정보가 저장되었습니다.");
    await waitFor(() =>
      expect(fetchMock).toHaveBeenNthCalledWith(
        2,
        "/api/menus/00000000-0000-0000-0000-000000000307",
        expect.objectContaining({ method: "PUT" }),
      ),
    );
    expect(screen.getByText("SCR-MENU-EXECUTION-MANAGEMENT")).toBeTruthy();
  });

  it("keeps the form open and shows a field error when screen ID is missing", async () => {
    vi.spyOn(globalThis, "fetch").mockResolvedValueOnce(jsonResponse(menus));

    render(<MenuInformationManagementPage />);
    await screen.findByRole("button", { name: "메뉴 정보 관리 실행정보 편집" });
    fireEvent.click(
      screen.getByRole("button", { name: "메뉴 정보 관리 실행정보 편집" }),
    );
    fireEvent.change(screen.getByLabelText("화면ID"), {
      target: { value: "" },
    });
    fireEvent.click(screen.getByRole("button", { name: "저장" }));

    expect(screen.getByText("화면ID를 입력하세요.")).toBeTruthy();
    expect(screen.getByRole("heading", { name: /실행정보 편집/ })).toBeTruthy();
  });
});

function jsonResponse(body: unknown, status = 200) {
  return new Response(JSON.stringify(body), {
    status,
    headers: { "Content-Type": "application/json" },
  });
}
