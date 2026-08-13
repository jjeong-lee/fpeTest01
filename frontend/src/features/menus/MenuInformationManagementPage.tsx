import { FormEvent, useEffect, useState } from "react";
import { api, type ApiError } from "../../api/client";

type MenuInformation = {
  menuId: string;
  menuName: string;
  screenId: string;
  url: string;
  icon: string | null;
  businessCategory: string | null;
  description: string | null;
  useStatus: string;
};

type Form = {
  menuName: string;
  screenId: string;
  url: string;
  icon: string;
  businessCategory: string;
  description: string;
  reason: string;
};
const emptyForm: Form = {
  menuName: "",
  screenId: "",
  url: "",
  icon: "",
  businessCategory: "",
  description: "",
  reason: "",
};

export function MenuInformationManagementPage() {
  const [menus, setMenus] = useState<MenuInformation[]>([]);
  const [state, setState] = useState<
    "loading" | "idle" | "error" | "permission"
  >("loading");
  const [selected, setSelected] = useState<MenuInformation | null>(null);
  const [form, setForm] = useState<Form>(emptyForm);
  const [formError, setFormError] = useState("");
  const [message, setMessage] = useState("");

  async function load(clearMessage = true) {
    setState("loading");
    if (clearMessage) setMessage("");
    try {
      const response = await api<MenuInformation[]>("/api/menus");
      setMenus(response.data);
      setSelected((current) =>
        current
          ? (response.data.find((menu) => menu.menuId === current.menuId) ??
            null)
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

  function openEdit(menu: MenuInformation) {
    setSelected(menu);
    setForm({
      menuName: menu.menuName,
      screenId: menu.screenId ?? "",
      url: menu.url ?? "",
      icon: menu.icon ?? "",
      businessCategory: menu.businessCategory ?? "",
      description: menu.description ?? "",
      reason: "",
    });
    setFormError("");
  }

  function updateField(field: keyof Form, value: string) {
    setForm((current) => ({ ...current, [field]: value }));
  }

  async function save(event: FormEvent) {
    event.preventDefault();
    if (!selected) return;
    if (!form.menuName.trim()) return setFormError("메뉴명을 입력하세요.");
    if (!form.screenId.trim()) return setFormError("화면ID를 입력하세요.");
    if (!form.url.trim()) return setFormError("URL을 입력하세요.");
    if (!form.reason.trim()) return setFormError("변경 사유를 입력하세요.");
    try {
      await api<MenuInformation>(`/api/menus/${selected.menuId}`, {
        method: "PUT",
        body: JSON.stringify({
          ...form,
          menuName: form.menuName.trim(),
          screenId: form.screenId.trim(),
          url: form.url.trim(),
          reason: form.reason.trim(),
        }),
      });
      setSelected(null);
      setForm(emptyForm);
      setMessage("메뉴 실행정보가 저장되었습니다.");
      await load(false);
    } catch (error) {
      const apiError = error as ApiError;
      if (apiError.error?.code === "MENU_ACCESS_DENIED") setState("permission");
      else
        setFormError(
          apiError.error?.fieldErrors?.[0]?.message ??
            apiError.error?.message ??
            "메뉴 실행정보를 저장할 수 없습니다.",
        );
    }
  }

  return (
    <section
      className="menu-information-management"
      aria-labelledby="menu-information-management-title"
    >
      <div className="page-title-row">
        <div>
          <p className="eyebrow">시스템 관리 · 메뉴 관리</p>
          <h1 id="menu-information-management-title">메뉴 정보 관리</h1>
          <p>메뉴 실행정보와 연결된 화면을 관리합니다.</p>
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
          <h2>메뉴 정보 관리 권한이 없습니다.</h2>
          <p>현재 계정에는 이 메뉴를 사용할 권한이 없습니다.</p>
        </section>
      ) : state === "error" ? (
        <section className="status-card" role="alert">
          <h2>메뉴 실행정보를 불러올 수 없습니다.</h2>
          <button
            type="button"
            className="secondary-button"
            onClick={() => void load()}
          >
            다시 시도
          </button>
        </section>
      ) : (
        <>
          <section className="menu-information-list-panel">
            <div className="list-header">
              <h2>메뉴 실행정보</h2>
              <span>{menus.length}건</span>
            </div>
            {state === "loading" ? (
              <div className="skeleton-line" />
            ) : menus.length === 0 ? (
              <p className="empty-state">등록된 메뉴 실행정보가 없습니다.</p>
            ) : (
              <div className="table-wrap">
                <table>
                  <thead>
                    <tr>
                      <th>메뉴명</th>
                      <th>화면ID</th>
                      <th>URL</th>
                      <th>아이콘</th>
                      <th>업무구분</th>
                      <th>설명</th>
                      <th>관리</th>
                    </tr>
                  </thead>
                  <tbody>
                    {menus.map((menu) => (
                      <tr key={menu.menuId}>
                        <td>{menu.menuName}</td>
                        <td>{menu.screenId}</td>
                        <td>{menu.url}</td>
                        <td>{menu.icon || "-"}</td>
                        <td>{menu.businessCategory || "-"}</td>
                        <td>{menu.description || "-"}</td>
                        <td>
                          <button
                            type="button"
                            className="text-button"
                            aria-label={`${menu.menuName} 실행정보 편집`}
                            onClick={() => openEdit(menu)}
                          >
                            실행정보 편집
                          </button>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
          </section>
          {selected && (
            <form
              className="menu-information-editor-panel"
              onSubmit={save}
              noValidate
            >
              <div className="list-header">
                <h2>
                  실행정보 편집 <span>{selected.menuName}</span>
                </h2>
                <button
                  type="button"
                  className="text-button"
                  onClick={() => {
                    setSelected(null);
                    setForm(emptyForm);
                    setFormError("");
                  }}
                >
                  취소
                </button>
              </div>
              <div className="execution-form-grid">
                <label>
                  메뉴명
                  <input
                    aria-label="메뉴명"
                    value={form.menuName}
                    onChange={(event) =>
                      updateField("menuName", event.target.value)
                    }
                  />
                </label>
                <label>
                  화면ID
                  <input
                    aria-label="화면ID"
                    value={form.screenId}
                    onChange={(event) =>
                      updateField("screenId", event.target.value)
                    }
                  />
                </label>
                <label>
                  URL
                  <input
                    aria-label="URL"
                    value={form.url}
                    onChange={(event) => updateField("url", event.target.value)}
                  />
                </label>
                <label>
                  아이콘
                  <input
                    aria-label="아이콘"
                    value={form.icon}
                    onChange={(event) =>
                      updateField("icon", event.target.value)
                    }
                  />
                </label>
                <label>
                  업무구분
                  <input
                    aria-label="업무구분"
                    value={form.businessCategory}
                    onChange={(event) =>
                      updateField("businessCategory", event.target.value)
                    }
                  />
                </label>
                <label className="execution-form-wide">
                  설명
                  <textarea
                    aria-label="설명"
                    value={form.description}
                    onChange={(event) =>
                      updateField("description", event.target.value)
                    }
                  />
                </label>
                <label className="execution-form-wide">
                  변경 사유
                  <textarea
                    aria-label="변경 사유"
                    value={form.reason}
                    onChange={(event) =>
                      updateField("reason", event.target.value)
                    }
                    required
                  />
                </label>
              </div>
              {formError && (
                <p className="form-error" role="alert">
                  {formError}
                </p>
              )}
              <div className="modal-actions">
                <button
                  type="button"
                  className="secondary-button"
                  onClick={() => {
                    setSelected(null);
                    setForm(emptyForm);
                    setFormError("");
                  }}
                >
                  취소
                </button>
                <button type="submit" className="primary-button">
                  저장
                </button>
              </div>
            </form>
          )}
        </>
      )}
    </section>
  );
}
