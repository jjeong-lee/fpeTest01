import { FormEvent, useEffect, useMemo, useState } from "react";
import { api, type ApiError } from "../../api/client";

type MenuNode = {
  menuId: string;
  parentMenuId: string | null;
  menuName: string;
  displayOrder: number;
  useStatus: string;
  children: MenuNode[];
};

type Modal = "parent" | "order" | null;

export function MenuStructureManagementPage() {
  const [menus, setMenus] = useState<MenuNode[]>([]);
  const [state, setState] = useState<
    "loading" | "idle" | "error" | "permission"
  >("loading");
  const [selected, setSelected] = useState<MenuNode | null>(null);
  const [modal, setModal] = useState<Modal>(null);
  const [parentMenuId, setParentMenuId] = useState("");
  const [displayOrder, setDisplayOrder] = useState("");
  const [reason, setReason] = useState("");
  const [formError, setFormError] = useState("");
  const [message, setMessage] = useState("");

  const allMenus = useMemo(() => flatten(menus), [menus]);

  async function load(clearMessage = true) {
    setState("loading");
    if (clearMessage) setMessage("");
    try {
      const response = await api<MenuNode[]>("/api/menu-structure");
      setMenus(response.data);
      setSelected((current) =>
        current
          ? (flatten(response.data).find(
              (menu) => menu.menuId === current.menuId,
            ) ?? null)
          : null,
      );
      setState("idle");
    } catch (error) {
      setState(
        (error as ApiError).error?.code === "MENU_ACCESS_DENIED"
          ? "permission"
          : "error",
      );
    }
  }

  useEffect(() => {
    void load();
  }, []);

  function openModal(nextModal: Exclude<Modal, null>) {
    if (!selected) return;
    setFormError("");
    setReason("");
    setParentMenuId(selected.parentMenuId ?? "");
    setDisplayOrder(String(selected.displayOrder));
    setModal(nextModal);
  }

  async function save(event: FormEvent) {
    event.preventDefault();
    if (!selected || !modal) return;
    if (!reason.trim()) {
      setFormError("변경 사유를 입력하세요.");
      return;
    }
    if (modal === "parent" && !parentMenuId) {
      setFormError("부모메뉴를 선택하세요.");
      return;
    }
    if (
      modal === "order" &&
      (displayOrder.trim() === "" ||
        Number(displayOrder) < 0 ||
        !Number.isInteger(Number(displayOrder)))
    ) {
      setFormError("표시순서는 0 이상의 정수로 입력하세요.");
      return;
    }
    try {
      const path =
        modal === "parent"
          ? `/api/menus/${selected.menuId}/parent`
          : `/api/menus/${selected.menuId}/display-order`;
      const body =
        modal === "parent"
          ? { parentMenuId, reason: reason.trim() }
          : { displayOrder: Number(displayOrder), reason: reason.trim() };
      await api<MenuNode>(path, { method: "PUT", body: JSON.stringify(body) });
      setModal(null);
      setMessage("메뉴 구조가 저장되었습니다.");
      await load(false);
    } catch (error) {
      const apiError = error as ApiError;
      if (apiError.error?.code === "MENU_ACCESS_DENIED") setState("permission");
      else
        setFormError(
          apiError.error?.fieldErrors?.[0]?.message ??
            apiError.error?.message ??
            "메뉴 구조를 저장할 수 없습니다.",
        );
    }
  }

  return (
    <section
      className="menu-structure-management"
      aria-labelledby="menu-structure-management-title"
    >
      <div className="page-title-row">
        <div>
          <p className="eyebrow">시스템 관리 · 메뉴 관리</p>
          <h1 id="menu-structure-management-title">메뉴 구조 관리</h1>
          <p>메뉴의 부모-자식 관계와 동일 계층의 표시순서를 관리합니다.</p>
        </div>
        <button
          type="button"
          className="secondary-button"
          onClick={() => void load()}
        >
          새로고침
        </button>
      </div>
      {message && (
        <p className="success-message" role="status">
          {message}
        </p>
      )}
      {state === "permission" ? (
        <section className="status-card permission-state" role="alert">
          <h2>메뉴 구조 관리 권한이 없습니다.</h2>
          <p>현재 계정에는 이 메뉴를 사용할 권한이 없습니다.</p>
        </section>
      ) : state === "error" ? (
        <section className="status-card" role="alert">
          <h2>메뉴 구조를 불러올 수 없습니다.</h2>
          <button
            type="button"
            className="secondary-button"
            onClick={() => void load()}
          >
            다시 시도
          </button>
        </section>
      ) : (
        <section className="menu-structure-panel">
          <div className="list-header">
            <h2>메뉴 트리</h2>
            <span>{allMenus.length}건</span>
          </div>
          {state === "loading" ? (
            <div className="skeleton-line" />
          ) : menus.length === 0 ? (
            <p className="empty-state">등록된 메뉴 구조가 없습니다.</p>
          ) : (
            <div className="menu-tree">
              {menus.map((menu) => (
                <MenuBranch
                  key={menu.menuId}
                  menu={menu}
                  selectedId={selected?.menuId}
                  onSelect={setSelected}
                />
              ))}
            </div>
          )}
          <section className="menu-selection-panel">
            <h2>선택 메뉴</h2>
            {selected ? (
              <>
                <p>
                  <strong>{selected.menuName}</strong>
                  <span> · 표시순서 {selected.displayOrder}</span>
                </p>
                <div className="row-actions">
                  <button
                    type="button"
                    className="secondary-button"
                    onClick={() => openModal("parent")}
                  >
                    부모메뉴 변경
                  </button>
                  <button
                    type="button"
                    className="primary-button"
                    onClick={() => openModal("order")}
                  >
                    순서 재정렬
                  </button>
                </div>
              </>
            ) : (
              <p className="empty-state">트리에서 메뉴를 선택하세요.</p>
            )}
          </section>
        </section>
      )}
      {modal && selected && (
        <div className="modal-backdrop" role="presentation">
          <form
            className="settings-modal"
            aria-label={
              modal === "parent" ? "부모메뉴 변경" : "표시순서 재정렬"
            }
            role="dialog"
            onSubmit={save}
          >
            <div className="modal-heading">
              <div>
                <p className="eyebrow">{selected.menuName}</p>
                <h2>
                  {modal === "parent" ? "부모메뉴 변경" : "표시순서 재정렬"}
                </h2>
              </div>
              <button
                type="button"
                className="icon-button"
                aria-label="닫기"
                onClick={() => setModal(null)}
              >
                ×
              </button>
            </div>
            {modal === "parent" ? (
              <label>
                부모메뉴
                <select
                  aria-label="부모메뉴"
                  value={parentMenuId}
                  onChange={(event) => setParentMenuId(event.target.value)}
                >
                  <option value="">선택하세요</option>
                  {allMenus
                    .filter((menu) => menu.menuId !== selected.menuId)
                    .map((menu) => (
                      <option key={menu.menuId} value={menu.menuId}>
                        {menu.menuName}
                      </option>
                    ))}
                </select>
              </label>
            ) : (
              <label>
                동일 계층 표시순서
                <input
                  aria-label="동일 계층 표시순서"
                  type="number"
                  min="0"
                  value={displayOrder}
                  onChange={(event) => setDisplayOrder(event.target.value)}
                  required
                />
              </label>
            )}
            <label>
              변경 사유
              <textarea
                aria-label="변경 사유"
                value={reason}
                onChange={(event) => setReason(event.target.value)}
                required
              />
            </label>
            {formError && (
              <p className="form-error" role="alert">
                {formError}
              </p>
            )}
            <div className="modal-actions">
              <button
                type="button"
                className="secondary-button"
                onClick={() => setModal(null)}
              >
                취소
              </button>
              <button type="submit" className="primary-button">
                저장
              </button>
            </div>
          </form>
        </div>
      )}
    </section>
  );
}

function MenuBranch({
  menu,
  selectedId,
  onSelect,
}: {
  menu: MenuNode;
  selectedId?: string;
  onSelect: (menu: MenuNode) => void;
}) {
  return (
    <div className="menu-branch">
      <div
        className={
          selectedId === menu.menuId
            ? "menu-tree-row selected-row"
            : "menu-tree-row"
        }
      >
        <span>{menu.menuName}</span>
        <span className="menu-order">순서 {menu.displayOrder}</span>
        <button
          type="button"
          className="text-button"
          onClick={() => onSelect(menu)}
        >
          {menu.menuName} 선택
        </button>
      </div>
      {menu.children.length > 0 && (
        <div className="menu-tree-children">
          {menu.children.map((child) => (
            <MenuBranch
              key={child.menuId}
              menu={child}
              selectedId={selectedId}
              onSelect={onSelect}
            />
          ))}
        </div>
      )}
    </div>
  );
}

function flatten(nodes: MenuNode[]): MenuNode[] {
  return nodes.flatMap((node) => [node, ...flatten(node.children)]);
}
