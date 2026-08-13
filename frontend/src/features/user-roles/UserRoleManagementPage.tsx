import { FormEvent, useState } from "react";
import { api, type ApiError } from "../../api/client";

type UserRole = {
  roleCode: string;
  assignmentType: string;
  effectiveStartDate: string | null;
  effectiveEndDate: string | null;
  approverUserId: string;
  reason: string;
  status: string;
};

type FormState = {
  roleCode: string;
  assignmentType: string;
  effectiveStartDate: string;
  effectiveEndDate: string;
  approverUserId: string;
  reason: string;
};

const emptyForm: FormState = {
  roleCode: "",
  assignmentType: "MANUAL",
  effectiveStartDate: "",
  effectiveEndDate: "",
  approverUserId: "",
  reason: "",
};

export function UserRoleManagementPage() {
  const [userId, setUserId] = useState("");
  const [roles, setRoles] = useState<UserRole[]>([]);
  const [form, setForm] = useState<FormState>(emptyForm);
  const [state, setState] = useState<
    "idle" | "loading" | "error" | "permission"
  >("idle");
  const [message, setMessage] = useState("");
  const [formError, setFormError] = useState("");
  const [revokeTarget, setRevokeTarget] = useState<UserRole | null>(null);
  const [revokeReason, setRevokeReason] = useState("");
  const [revokeError, setRevokeError] = useState("");

  async function loadRoles(clearMessage = true) {
    if (!userId.trim()) {
      setFormError("사용자 식별자를 입력하세요.");
      return;
    }
    setState("loading");
    if (clearMessage) setMessage("");
    try {
      const response = await api<{ content: UserRole[] }>(
        `/api/users/${userId}/roles`,
      );
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

  async function save(event: FormEvent) {
    event.preventDefault();
    setFormError("");
    if (!userId.trim() || !form.roleCode.trim()) {
      setFormError("사용자 식별자와 역할코드를 입력하세요.");
      return;
    }
    try {
      await api<UserRole>(`/api/users/${userId}/roles/${form.roleCode}`, {
        method: "PUT",
        body: JSON.stringify({
          assignmentType: form.assignmentType,
          effectiveStartDate: form.effectiveStartDate || undefined,
          effectiveEndDate: form.effectiveEndDate || undefined,
          approverUserId: form.approverUserId,
          reason: form.reason,
        }),
      });
      setMessage("역할이 저장되었습니다.");
      setForm(emptyForm);
      await loadRoles(false);
    } catch (error) {
      const apiError = error as ApiError;
      setFormError(
        apiError.error?.fieldErrors?.[0]?.message ??
          apiError.error?.message ??
          "역할을 저장할 수 없습니다.",
      );
    }
  }

  function editRole(role: UserRole) {
    setForm({
      roleCode: role.roleCode,
      assignmentType: role.assignmentType,
      effectiveStartDate: role.effectiveStartDate ?? "",
      effectiveEndDate: role.effectiveEndDate ?? "",
      approverUserId: role.approverUserId,
      reason: "",
    });
    setFormError("");
  }

  async function confirmRevocation() {
    if (!revokeTarget) return;
    setRevokeError("");
    try {
      await api<UserRole>(
        `/api/users/${userId}/roles/${revokeTarget.roleCode}/revocation`,
        { method: "POST", body: JSON.stringify({ reason: revokeReason }) },
      );
      setRevokeTarget(null);
      setRevokeReason("");
      setMessage("역할이 회수되었습니다.");
      await loadRoles(false);
    } catch (error) {
      const apiError = error as ApiError;
      setRevokeError(
        apiError.error?.fieldErrors?.[0]?.message ??
          apiError.error?.message ??
          "역할을 회수할 수 없습니다.",
      );
    }
  }

  return (
    <section
      className="user-role-management"
      aria-labelledby="user-role-management-title"
    >
      <div className="page-title-row">
        <div>
          <p className="eyebrow">시스템 관리 · 역할·권한 관리</p>
          <h1 id="user-role-management-title">사용자 역할 관리</h1>
          <p>
            사용자별 현재 역할과 유효기간을 확인하고 역할을
            부여·변경·회수합니다.
          </p>
        </div>
      </div>
      <section className="search-card user-role-search">
        <label>
          사용자 식별자
          <input
            aria-label="사용자 식별자"
            value={userId}
            onChange={(event) => setUserId(event.target.value)}
            placeholder="UUID"
          />
        </label>
        <button
          type="button"
          className="primary-button"
          onClick={() => void loadRoles()}
        >
          현재 역할 조회
        </button>
      </section>
      {message && (
        <p className="success-message" role="status">
          {message}
        </p>
      )}
      {state === "permission" ? (
        <section className="status-card permission-state" role="alert">
          <h2>사용자 역할 관리 권한이 없습니다.</h2>
          <p>현재 계정에는 이 메뉴를 사용할 권한이 없습니다.</p>
        </section>
      ) : state === "error" ? (
        <section className="status-card" role="alert">
          <h2>현재 역할을 불러올 수 없습니다.</h2>
          <button
            type="button"
            className="secondary-button"
            onClick={() => void loadRoles()}
          >
            다시 조회
          </button>
        </section>
      ) : (
        <div className="user-role-workspace">
          <section className="user-role-list-panel">
            <div className="list-header">
              <h2>현재 역할</h2>
              <span>{roles.length}건</span>
            </div>
            {state === "loading" ? (
              <div className="skeleton-line" />
            ) : roles.length === 0 ? (
              <p className="empty-state">현재 역할이 없습니다.</p>
            ) : (
              <div className="table-wrap">
                <table>
                  <thead>
                    <tr>
                      <th>역할코드</th>
                      <th>부여 구분</th>
                      <th>유효 시작일</th>
                      <th>유효 종료일</th>
                      <th>승인자</th>
                      <th>처리</th>
                    </tr>
                  </thead>
                  <tbody>
                    {roles.map((role) => (
                      <tr key={role.roleCode}>
                        <td>{role.roleCode}</td>
                        <td>
                          {role.assignmentType === "POSITION_BASED"
                            ? "보직 기반"
                            : "수동"}
                        </td>
                        <td>{role.effectiveStartDate ?? "-"}</td>
                        <td>{role.effectiveEndDate ?? "-"}</td>
                        <td>{role.approverUserId}</td>
                        <td>
                          <div className="row-actions">
                            <button
                              type="button"
                              className="text-button"
                              onClick={() => editRole(role)}
                            >
                              {role.roleCode} 변경
                            </button>
                            <button
                              type="button"
                              className="text-button danger-text"
                              onClick={() => {
                                setRevokeTarget(role);
                                setRevokeReason("");
                                setRevokeError("");
                              }}
                            >
                              {role.roleCode} 회수
                            </button>
                          </div>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
          </section>
          <aside className="user-role-editor-panel">
            <p className="eyebrow">역할 부여 또는 변경</p>
            <h2>역할 정보</h2>
            <form onSubmit={save}>
              <label>
                역할코드
                <input
                  aria-label="역할코드"
                  value={form.roleCode}
                  onChange={(event) =>
                    setForm({ ...form, roleCode: event.target.value })
                  }
                  placeholder="R01~R09"
                  required
                />
              </label>
              <label>
                부여 구분
                <select
                  aria-label="부여 구분"
                  value={form.assignmentType}
                  onChange={(event) =>
                    setForm({ ...form, assignmentType: event.target.value })
                  }
                >
                  <option value="MANUAL">수동</option>
                  <option value="POSITION_BASED">보직 기반</option>
                </select>
              </label>
              <label>
                유효 시작일
                <input
                  aria-label="유효 시작일"
                  type="date"
                  value={form.effectiveStartDate}
                  onChange={(event) =>
                    setForm({ ...form, effectiveStartDate: event.target.value })
                  }
                />
              </label>
              <label>
                유효 종료일
                <input
                  aria-label="유효 종료일"
                  type="date"
                  value={form.effectiveEndDate}
                  onChange={(event) =>
                    setForm({ ...form, effectiveEndDate: event.target.value })
                  }
                />
              </label>
              <label>
                승인자
                <input
                  aria-label="승인자"
                  value={form.approverUserId}
                  onChange={(event) =>
                    setForm({ ...form, approverUserId: event.target.value })
                  }
                  required
                />
              </label>
              <label>
                처리 사유
                <textarea
                  aria-label="처리 사유"
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
                <button
                  type="button"
                  className="secondary-button"
                  onClick={() => {
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
          </aside>
        </div>
      )}
      {revokeTarget && (
        <div className="modal-backdrop" role="presentation">
          <section
            className="settings-modal"
            role="dialog"
            aria-modal="true"
            aria-labelledby="revocation-title"
          >
            <div className="modal-heading">
              <div>
                <p className="eyebrow">{revokeTarget.roleCode}</p>
                <h2 id="revocation-title">역할 회수 확인</h2>
              </div>
              <button
                type="button"
                className="icon-button"
                aria-label="닫기"
                onClick={() => setRevokeTarget(null)}
              >
                ×
              </button>
            </div>
            <p className="source-notice">
              회수한 역할은 현재 역할 목록에서 제외되며 처리 이력은 보존됩니다.
            </p>
            <label>
              회수 사유
              <textarea
                aria-label="회수 사유"
                value={revokeReason}
                onChange={(event) => setRevokeReason(event.target.value)}
                required
              />
            </label>
            {revokeError && (
              <p className="form-error" role="alert">
                {revokeError}
              </p>
            )}
            <div className="modal-actions">
              <button
                type="button"
                className="secondary-button"
                onClick={() => setRevokeTarget(null)}
              >
                취소
              </button>
              <button
                type="button"
                className="primary-button"
                onClick={confirmRevocation}
              >
                회수 확정
              </button>
            </div>
          </section>
        </div>
      )}
    </section>
  );
}
