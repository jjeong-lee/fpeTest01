import { FormEvent, useEffect, useState } from "react";
import { api, type ApiError } from "../../api/client";

type Role = {
  roleCode: string;
  roleName: string;
  purpose: string;
  grantCriteria: string | null;
  dataScopeDefault: string | null;
  useStatus: string;
};

type SearchData = { content: Role[]; totalElements: number };
type FormState = {
  roleName: string;
  grantCriteria: string;
  dataScopeDefault: string;
  reason: string;
};

export function RoleManagementPage() {
  const [roles, setRoles] = useState<Role[]>([]);
  const [selected, setSelected] = useState<Role | null>(null);
  const [state, setState] = useState<
    "idle" | "loading" | "error" | "permission"
  >("idle");
  const [message, setMessage] = useState("");
  const [formError, setFormError] = useState("");
  const [form, setForm] = useState<FormState>({
    roleName: "",
    grantCriteria: "",
    dataScopeDefault: "",
    reason: "",
  });

  async function search() {
    setState("loading");
    setMessage("");
    try {
      const response = await api<SearchData>("/api/roles");
      setRoles(response.data.content);
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
    void search();
  }, []);

  function selectRole(role: Role) {
    setSelected(role);
    setForm({
      roleName: role.roleName,
      grantCriteria: role.grantCriteria ?? "",
      dataScopeDefault: role.dataScopeDefault ?? "",
      reason: "",
    });
    setFormError("");
    setMessage("");
  }

  async function save(event: FormEvent) {
    event.preventDefault();
    if (!selected) return;
    setFormError("");
    try {
      const response = await api<Role>(`/api/roles/${selected.roleCode}`, {
        method: "PUT",
        body: JSON.stringify({
          roleName: form.roleName,
          grantCriteria: form.grantCriteria,
          dataScopeDefault: form.dataScopeDefault,
          reason: form.reason,
        }),
      });
      setSelected(response.data);
      setRoles((current) =>
        current.map((role) =>
          role.roleCode === response.data.roleCode ? response.data : role,
        ),
      );
      await search();
      setMessage("역할 정책이 저장되었습니다.");
    } catch (error) {
      const apiError = error as ApiError;
      setFormError(
        apiError.error?.fieldErrors?.[0]?.message ??
          apiError.error?.message ??
          "저장할 수 없습니다.",
      );
    }
  }

  return (
    <section
      className="role-management"
      aria-labelledby="role-management-title"
    >
      <div className="page-title-row">
        <div>
          <p className="eyebrow">시스템 관리 · 역할·권한 관리</p>
          <h1 id="role-management-title">역할 관리</h1>
          <p>역할코드는 고정하고 역할명과 운영 정책을 관리합니다.</p>
        </div>
        <button type="button" className="primary-button" onClick={search}>
          역할 목록 조회
        </button>
      </div>
      {message && (
        <p className="success-message" role="status">
          {message}
        </p>
      )}
      {state === "permission" ? (
        <section className="status-card permission-state" role="alert">
          <h2>역할 관리 권한이 없습니다.</h2>
          <p>현재 계정에는 이 메뉴를 사용할 권한이 없습니다.</p>
        </section>
      ) : state === "error" ? (
        <section className="status-card" role="alert">
          <h2>역할 목록을 불러올 수 없습니다.</h2>
          <button type="button" className="secondary-button" onClick={search}>
            다시 시도
          </button>
        </section>
      ) : (
        <div className="role-workspace">
          <section className="role-list-panel">
            <div className="list-header">
              <h2>역할 목록</h2>
              <span>{roles.length}건</span>
            </div>
            {state === "loading" ? (
              <div className="skeleton-line" />
            ) : roles.length === 0 ? (
              <p className="empty-state">역할 목록을 조회하세요.</p>
            ) : (
              <div className="table-wrap">
                <table>
                  <thead>
                    <tr>
                      <th>역할코드</th>
                      <th>역할명</th>
                      <th>목적</th>
                      <th>사용 상태</th>
                      <th>처리</th>
                    </tr>
                  </thead>
                  <tbody>
                    {roles.map((role) => (
                      <tr
                        key={role.roleCode}
                        className={
                          selected?.roleCode === role.roleCode
                            ? "selected-row"
                            : ""
                        }
                      >
                        <td>{role.roleCode}</td>
                        <td>{role.roleName}</td>
                        <td>{role.purpose}</td>
                        <td>{role.useStatus}</td>
                        <td>
                          <button
                            type="button"
                            className="text-button"
                            onClick={() => selectRole(role)}
                          >
                            {role.roleCode} 선택
                          </button>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
          </section>
          <aside className="role-editor-panel">
            {selected ? (
              <form onSubmit={save}>
                <p className="eyebrow">선택 역할</p>
                <h2>
                  {selected.roleCode} <span>역할코드 변경 불가</span>
                </h2>
                <p className="role-purpose">{selected.purpose}</p>
                <label>
                  역할명
                  <input
                    aria-label="역할명"
                    value={form.roleName}
                    onChange={(event) =>
                      setForm({ ...form, roleName: event.target.value })
                    }
                  />
                </label>
                <label>
                  부여 기준
                  <textarea
                    aria-label="부여 기준"
                    value={form.grantCriteria}
                    onChange={(event) =>
                      setForm({ ...form, grantCriteria: event.target.value })
                    }
                  />
                </label>
                <label>
                  데이터 범위 기본값
                  <textarea
                    aria-label="데이터 범위 기본값"
                    value={form.dataScopeDefault}
                    onChange={(event) =>
                      setForm({ ...form, dataScopeDefault: event.target.value })
                    }
                  />
                </label>
                <label>
                  변경 사유
                  <textarea
                    aria-label="변경 사유"
                    value={form.reason}
                    onChange={(event) =>
                      setForm({ ...form, reason: event.target.value })
                    }
                    required
                  />
                </label>
                {formError && (
                  <p className="form-error" role="alert">
                    {formError}
                  </p>
                )}
                <div className="modal-actions">
                  <button type="submit" className="primary-button">
                    저장
                  </button>
                </div>
              </form>
            ) : (
              <p className="empty-state">목록에서 역할을 선택하세요.</p>
            )}
          </aside>
        </div>
      )}
    </section>
  );
}
