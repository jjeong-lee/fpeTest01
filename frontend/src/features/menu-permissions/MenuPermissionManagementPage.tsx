import { FormEvent, useState } from "react";
import { api, type ApiError } from "../../api/client";

type Permission = {
  subjectType: string;
  subjectId: string;
  menuId: string;
  topMenuName: string | null;
  middleMenuName: string | null;
  menuName: string;
  accessDecision: "ALLOW" | "DENY";
};

export function MenuPermissionManagementPage() {
  const [subjectType, setSubjectType] = useState("ROLE");
  const [subjectId, setSubjectId] = useState("");
  const [permissions, setPermissions] = useState<Permission[]>([]);
  const [original, setOriginal] = useState<Permission[]>([]);
  const [state, setState] = useState<
    "idle" | "loading" | "error" | "permission"
  >("idle");
  const [message, setMessage] = useState("");
  const [formError, setFormError] = useState("");
  const [reason, setReason] = useState("");

  async function loadPermissions(clearMessage = true) {
    if (!subjectId.trim()) {
      setFormError("대상 식별자를 입력하세요.");
      return;
    }
    setState("loading");
    if (clearMessage) setMessage("");
    setFormError("");
    try {
      const params = new URLSearchParams({
        subjectType,
        subjectId: subjectId.trim(),
      });
      const response = await api<{ content: Permission[] }>(
        `/api/menu-permissions?${params.toString()}`,
      );
      setPermissions(response.data.content);
      setOriginal(response.data.content);
      setState("idle");
    } catch (error) {
      setState(
        (error as ApiError).error?.code === "MENU_ACCESS_DENIED"
          ? "permission"
          : "error",
      );
    }
  }

  function changeDecision(menuId: string, accessDecision: "ALLOW" | "DENY") {
    setPermissions((current) =>
      current.map((permission) =>
        permission.menuId === menuId
          ? { ...permission, accessDecision }
          : permission,
      ),
    );
  }

  async function save(event: FormEvent) {
    event.preventDefault();
    const changed = permissions.find(
      (permission, index) =>
        permission.accessDecision !== original[index]?.accessDecision,
    );
    if (!changed) {
      setFormError("변경할 메뉴 권한을 선택하세요.");
      return;
    }
    if (!reason.trim()) {
      setFormError("변경 사유를 입력하세요.");
      return;
    }
    setFormError("");
    try {
      await api<Permission>("/api/menu-permissions", {
        method: "PUT",
        body: JSON.stringify({
          subjectType,
          subjectId: subjectId.trim(),
          menuId: changed.menuId,
          accessDecision: changed.accessDecision,
          reason: reason.trim(),
        }),
      });
      setReason("");
      setMessage("메뉴 권한이 저장되었습니다.");
      await loadPermissions(false);
    } catch (error) {
      const apiError = error as ApiError;
      setFormError(
        apiError.error?.fieldErrors?.[0]?.message ??
          apiError.error?.message ??
          "메뉴 권한을 저장할 수 없습니다.",
      );
    }
  }

  return (
    <section
      className="menu-permission-management"
      aria-labelledby="menu-permission-management-title"
    >
      <div className="page-title-row">
        <div>
          <p className="eyebrow">시스템 관리 · 역할·권한 관리</p>
          <h1 id="menu-permission-management-title">메뉴 권한 관리</h1>
          <p>역할·조직·사용자별 메뉴 접근 허용 여부를 관리합니다.</p>
        </div>
      </div>
      <section className="search-card permission-target-card">
        <label>
          대상 유형
          <select
            aria-label="대상 유형"
            value={subjectType}
            onChange={(event) => setSubjectType(event.target.value)}
          >
            <option value="ROLE">역할</option>
            <option value="ORGANIZATION">조직</option>
            <option value="USER">사용자</option>
          </select>
        </label>
        <label>
          대상 식별자
          <input
            aria-label="대상 식별자"
            value={subjectId}
            onChange={(event) => setSubjectId(event.target.value)}
          />
        </label>
        <button
          type="button"
          className="primary-button"
          onClick={() => void loadPermissions()}
        >
          권한 조회
        </button>
      </section>
      {message && (
        <p className="success-message" role="status">
          {message}
        </p>
      )}
      {state === "permission" ? (
        <section className="status-card permission-state" role="alert">
          <h2>메뉴 권한 관리 권한이 없습니다.</h2>
          <p>현재 계정에는 이 메뉴를 사용할 권한이 없습니다.</p>
        </section>
      ) : state === "error" ? (
        <section className="status-card" role="alert">
          <h2>메뉴 권한을 불러올 수 없습니다.</h2>
          <button
            type="button"
            className="secondary-button"
            onClick={() => void loadPermissions()}
          >
            다시 조회
          </button>
        </section>
      ) : (
        <section className="permission-matrix-panel">
          <div className="list-header">
            <h2>메뉴 접근 권한</h2>
            <span>{permissions.length}건</span>
          </div>
          {state === "loading" ? (
            <div className="skeleton-line" />
          ) : permissions.length === 0 ? (
            <p className="empty-state">
              선택한 대상에 설정된 메뉴 권한이 없습니다.
            </p>
          ) : (
            <form onSubmit={save}>
              <div className="table-wrap">
                <table>
                  <thead>
                    <tr>
                      <th>대메뉴</th>
                      <th>중메뉴</th>
                      <th>화면</th>
                      <th>접근 허용 여부</th>
                      <th>처리</th>
                    </tr>
                  </thead>
                  <tbody>
                    {permissions.map((permission) => (
                      <tr key={permission.menuId}>
                        <td>{permission.topMenuName ?? "-"}</td>
                        <td>{permission.middleMenuName ?? "-"}</td>
                        <td>{permission.menuName}</td>
                        <td>
                          {permission.accessDecision === "ALLOW"
                            ? "허용"
                            : "차단"}
                        </td>
                        <td>
                          <button
                            type="button"
                            className="text-button"
                            onClick={() =>
                              changeDecision(
                                permission.menuId,
                                permission.accessDecision === "ALLOW"
                                  ? "DENY"
                                  : "ALLOW",
                              )
                            }
                          >
                            {permission.menuName} 변경
                          </button>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
              <label className="permission-decision-field">
                접근 허용 여부
                <select
                  aria-label="접근 허용 여부"
                  value={
                    permissions.find(
                      (permission, index) =>
                        permission.accessDecision !==
                        original[index]?.accessDecision,
                    )?.accessDecision ??
                    permissions[0]?.accessDecision ??
                    "ALLOW"
                  }
                  onChange={(event) => {
                    const editable =
                      permissions.find(
                        (permission, index) =>
                          permission.accessDecision !==
                          original[index]?.accessDecision,
                      ) ?? permissions[0];
                    if (editable)
                      changeDecision(
                        editable.menuId,
                        event.target.value as "ALLOW" | "DENY",
                      );
                  }}
                >
                  <option value="ALLOW">허용</option>
                  <option value="DENY">차단</option>
                </select>
              </label>
              <label className="permission-decision-field">
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
                  onClick={() => {
                    setPermissions(original);
                    setReason("");
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
        </section>
      )}
    </section>
  );
}
