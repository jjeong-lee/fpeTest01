import { FormEvent, useMemo, useState } from "react";
import { api, type ApiError } from "../../api/client";

type User = {
  userId: string;
  employeeNo: string;
  name: string;
  organizationCode: string;
  position: string;
  employmentStatus: string;
  duty: string | null;
  retirementDate: string | null;
  lastSyncedAt: string;
  systemEnabled: boolean;
  roleCodes: string[];
};

type SearchData = { content: User[]; totalElements: number };
type SearchFields = Record<
  | "employeeNo"
  | "name"
  | "organizationCode"
  | "position"
  | "employmentStatus"
  | "roleCode"
  | "systemEnabled",
  string
>;

const emptyFields: SearchFields = {
  employeeNo: "",
  name: "",
  organizationCode: "",
  position: "",
  employmentStatus: "",
  roleCode: "",
  systemEnabled: "",
};

export function UserManagementPage() {
  const [filters, setFilters] = useState<SearchFields>(emptyFields);
  const [users, setUsers] = useState<User[]>([]);
  const [selected, setSelected] = useState<User | null>(null);
  const [state, setState] = useState<
    "idle" | "loading" | "error" | "permission"
  >("idle");
  const [message, setMessage] = useState("");
  const [settingsOpen, setSettingsOpen] = useState(false);
  const [systemEnabled, setSystemEnabled] = useState(true);
  const [roleCode, setRoleCode] = useState("");
  const [reason, setReason] = useState("");
  const [formError, setFormError] = useState("");

  const query = useMemo(
    () =>
      new URLSearchParams(
        Object.entries(filters).filter(([, value]) => value !== ""),
      ).toString(),
    [filters],
  );

  async function search(clearMessage = true) {
    setState("loading");
    if (clearMessage) setMessage("");
    try {
      const response = await api<SearchData>(
        `/api/users${query ? `?${query}` : ""}`,
      );
      setUsers(response.data.content);
      setSelected(
        (current) =>
          response.data.content.find(
            (user) => user.userId === current?.userId,
          ) ??
          response.data.content[0] ??
          null,
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

  function openSettings() {
    if (!selected) return;
    setSystemEnabled(selected.systemEnabled);
    setRoleCode(selected.roleCodes.join(","));
    setReason("");
    setFormError("");
    setSettingsOpen(true);
  }

  async function saveSettings(event: FormEvent) {
    event.preventDefault();
    if (!selected) return;
    setFormError("");
    try {
      await api(`/api/users/${selected.userId}/system-settings`, {
        method: "PATCH",
        body: JSON.stringify({
          systemEnabled,
          roleCodes: roleCode
            .split(",")
            .map((value) => value.trim())
            .filter(Boolean),
          reason,
        }),
      });
      setSettingsOpen(false);
      setMessage("시스템 설정이 저장되었습니다.");
      await search(false);
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
      className="user-management"
      aria-labelledby="user-management-title"
    >
      <div className="page-title-row">
        <div>
          <p className="eyebrow">시스템 관리 · 사용자·조직 관리</p>
          <h1 id="user-management-title">사용자 관리</h1>
          <p>
            원천 인사 정보는 조회하고, 시스템 사용 여부와 업무 역할만
            관리합니다.
          </p>
        </div>
      </div>
      <section className="search-card" aria-label="사용자 검색 조건">
        <div className="filter-grid">
          {(
            [
              "employeeNo",
              "name",
              "organizationCode",
              "position",
              "employmentStatus",
              "roleCode",
            ] as const
          ).map((field) => (
            <label key={field}>
              {labelFor(field)}
              <input
                value={filters[field]}
                onChange={(event) =>
                  setFilters({ ...filters, [field]: event.target.value })
                }
              />
            </label>
          ))}
          <label>
            시스템 사용 여부
            <select
              aria-label="시스템 사용 여부"
              value={filters.systemEnabled}
              onChange={(event) =>
                setFilters({ ...filters, systemEnabled: event.target.value })
              }
            >
              <option value="">전체</option>
              <option value="true">사용</option>
              <option value="false">미사용</option>
            </select>
          </label>
        </div>
        <div className="search-actions">
          <button
            type="button"
            className="secondary-button"
            onClick={() => {
              setFilters(emptyFields);
              setUsers([]);
              setSelected(null);
            }}
          >
            초기화
          </button>
          <button
            type="button"
            className="primary-button"
            onClick={() => void search()}
          >
            조회
          </button>
        </div>
      </section>
      {message && (
        <p className="success-message" role="status">
          {message}
        </p>
      )}
      {state === "permission" ? (
        <section className="status-card permission-state" role="alert">
          <h2>사용자 관리 권한이 없습니다.</h2>
          <p>현재 계정에는 이 메뉴를 사용할 권한이 없습니다.</p>
        </section>
      ) : state === "error" ? (
        <section className="status-card" role="alert">
          <h2>사용자 목록을 불러올 수 없습니다.</h2>
          <button
            type="button"
            className="secondary-button"
            onClick={() => void search()}
          >
            다시 시도
          </button>
        </section>
      ) : (
        <div className="user-workspace">
          <section className="user-list-panel">
            <div className="list-header">
              <h2>사용자 목록</h2>
              <span>{users.length}명</span>
            </div>
            {state === "loading" ? (
              <div className="skeleton-line" />
            ) : users.length === 0 ? (
              <p className="empty-state">검색 조건을 입력한 뒤 조회하세요.</p>
            ) : (
              <div className="table-wrap">
                <table>
                  <thead>
                    <tr>
                      <th>교번</th>
                      <th>성명</th>
                      <th>소속</th>
                      <th>직급</th>
                      <th>재직</th>
                      <th>보직</th>
                      <th>퇴직일</th>
                      <th>최종 동기화</th>
                      <th>사용</th>
                    </tr>
                  </thead>
                  <tbody>
                    {users.map((user) => (
                      <tr
                        key={user.userId}
                        className={
                          selected?.userId === user.userId ? "selected-row" : ""
                        }
                        onClick={() => setSelected(user)}
                      >
                        <td>{user.employeeNo}</td>
                        <td>{user.name}</td>
                        <td>{user.organizationCode}</td>
                        <td>{user.position}</td>
                        <td>{user.employmentStatus}</td>
                        <td>{user.duty ?? "-"}</td>
                        <td>{user.retirementDate ?? "-"}</td>
                        <td>{formatDateTime(user.lastSyncedAt)}</td>
                        <td>{user.systemEnabled ? "사용" : "미사용"}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
          </section>
          <aside className="source-detail">
            <p className="eyebrow">선택 사용자</p>
            {selected ? (
              <>
                <h2>
                  {selected.name} <span>{selected.employeeNo}</span>
                </h2>
                <dl>
                  <div>
                    <dt>보직</dt>
                    <dd>{selected.duty ?? "-"}</dd>
                  </div>
                  <div>
                    <dt>퇴직일자</dt>
                    <dd>{selected.retirementDate ?? "-"}</dd>
                  </div>
                  <div>
                    <dt>최종 동기화일시</dt>
                    <dd>{formatDateTime(selected.lastSyncedAt)}</dd>
                  </div>
                  <div>
                    <dt>현재 업무 역할</dt>
                    <dd>{selected.roleCodes.join(", ") || "-"}</dd>
                  </div>
                </dl>
                <button
                  type="button"
                  className="primary-button"
                  onClick={openSettings}
                >
                  시스템 설정 변경
                </button>
              </>
            ) : (
              <p className="empty-state">목록에서 사용자를 선택하세요.</p>
            )}
          </aside>
        </div>
      )}
      {settingsOpen && selected && (
        <div className="modal-backdrop" role="presentation">
          <form
            className="settings-modal"
            aria-label="시스템 설정 변경"
            onSubmit={saveSettings}
          >
            <div className="modal-heading">
              <div>
                <p className="eyebrow">
                  {selected.name} · {selected.employeeNo}
                </p>
                <h2>시스템 설정 변경</h2>
              </div>
              <button
                type="button"
                className="icon-button"
                aria-label="닫기"
                onClick={() => setSettingsOpen(false)}
              >
                ×
              </button>
            </div>
            <p className="source-notice">
              KORUS 원천 정보는 변경할 수 없습니다.
            </p>
            <label className="checkbox-label">
              <input
                aria-label="시스템 사용"
                type="checkbox"
                checked={systemEnabled}
                onChange={(event) => setSystemEnabled(event.target.checked)}
              />{" "}
              시스템 사용
            </label>
            <label>
              업무 역할
              <input
                aria-label="업무 역할"
                value={roleCode}
                onChange={(event) => setRoleCode(event.target.value)}
                placeholder="예: R01, R09"
              />
            </label>
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
                onClick={() => setSettingsOpen(false)}
              >
                취소
              </button>
              <button className="primary-button" type="submit">
                저장
              </button>
            </div>
          </form>
        </div>
      )}
    </section>
  );
}

function labelFor(field: keyof Omit<SearchFields, "systemEnabled">) {
  return {
    employeeNo: "교번",
    name: "성명",
    organizationCode: "소속",
    position: "직급",
    employmentStatus: "재직상태",
    roleCode: "역할",
  }[field];
}
function formatDateTime(value: string) {
  return value.replace("T", " ").replace(/([+-]\d\d:\d\d|Z)$/, "");
}
