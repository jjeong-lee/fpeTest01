import { FormEvent, useMemo, useState } from "react";
import { api, type ApiError } from "../../api/client";

type Organization = {
  organizationCode: string;
  organizationName: string;
  organizationType: string;
  useStatus: string;
  currentParentOrganizationCode: string | null;
};

type Relation = {
  organizationCode: string;
  parentOrganizationCode: string | null;
  effectiveStartDate: string;
  effectiveEndDate: string | null;
  status: string;
};

type Tree = Organization & {
  parent: Organization | null;
  children: Organization[];
  relationHistory: Relation[];
};
type SearchData = { content: Organization[]; totalElements: number };

export function OrganizationManagementPage() {
  const [organizationCode, setOrganizationCode] = useState("");
  const [organizations, setOrganizations] = useState<Organization[]>([]);
  const [selected, setSelected] = useState<Organization | null>(null);
  const [tree, setTree] = useState<Tree | null>(null);
  const [state, setState] = useState<
    "idle" | "loading" | "error" | "permission"
  >("idle");
  const [treeState, setTreeState] = useState<"idle" | "loading" | "error">(
    "idle",
  );
  const [modalOpen, setModalOpen] = useState(false);
  const [parentOrganizationCode, setParentOrganizationCode] = useState("");
  const [effectiveStartDate, setEffectiveStartDate] = useState("");
  const [effectiveEndDate, setEffectiveEndDate] = useState("");
  const [reason, setReason] = useState("");
  const [formError, setFormError] = useState("");
  const [message, setMessage] = useState("");

  const query = useMemo(
    () =>
      new URLSearchParams(
        organizationCode ? { organizationCode } : {},
      ).toString(),
    [organizationCode],
  );

  async function search(clearMessage = true) {
    setState("loading");
    if (clearMessage) setMessage("");
    try {
      const response = await api<SearchData>(
        `/api/organizations${query ? `?${query}` : ""}`,
      );
      setOrganizations(response.data.content);
      const next =
        response.data.content.find(
          (organization) =>
            organization.organizationCode === selected?.organizationCode,
        ) ??
        response.data.content[0] ??
        null;
      setSelected(next);
      setTree(null);
      setState("idle");
    } catch (error) {
      setState(
        (error as ApiError).error?.code === "MENU_ACCESS_DENIED"
          ? "permission"
          : "error",
      );
    }
  }

  async function loadTree(organization: Organization) {
    setSelected(organization);
    setTreeState("loading");
    try {
      const response = await api<Tree>(
        `/api/organizations/tree?${new URLSearchParams({ organizationCode: organization.organizationCode })}`,
      );
      setTree(response.data);
      setTreeState("idle");
    } catch {
      setTreeState("error");
    }
  }

  function openRelationModal() {
    if (!selected) return;
    const active = tree?.relationHistory.at(-1);
    setParentOrganizationCode(active?.parentOrganizationCode ?? "");
    setEffectiveStartDate("");
    setEffectiveEndDate("");
    setReason("");
    setFormError("");
    setModalOpen(true);
  }

  async function saveRelation(event: FormEvent) {
    event.preventDefault();
    if (!selected) return;
    setFormError("");
    try {
      const response = await api<Tree>(
        `/api/organization-relations/${selected.organizationCode}`,
        {
          method: "PUT",
          body: JSON.stringify({
            parentOrganizationCode: parentOrganizationCode || null,
            effectiveStartDate,
            effectiveEndDate: effectiveEndDate || null,
            reason,
          }),
        },
      );
      setTree(response.data);
      setModalOpen(false);
      setMessage("조직 관계가 저장되었습니다.");
      await search(false);
      await loadTree(selected);
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
      className="organization-management"
      aria-labelledby="organization-management-title"
    >
      <div className="page-title-row">
        <div>
          <p className="eyebrow">시스템 관리 · 사용자·조직 관리</p>
          <h1 id="organization-management-title">조직 관리</h1>
          <p>
            KORUS 조직 원천은 조회 전용으로 유지하고 관계 보정과 적용기간만
            관리합니다.
          </p>
        </div>
      </div>
      <section className="search-card" aria-label="조직 검색 조건">
        <div className="organization-search-row">
          <label>
            조직코드
            <input
              value={organizationCode}
              onChange={(event) => setOrganizationCode(event.target.value)}
            />
          </label>
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
          <h2>조직 관리 권한이 없습니다.</h2>
          <p>현재 계정에는 이 메뉴를 사용할 권한이 없습니다.</p>
        </section>
      ) : state === "error" ? (
        <section className="status-card" role="alert">
          <h2>조직 목록을 불러올 수 없습니다.</h2>
          <button
            type="button"
            className="secondary-button"
            onClick={() => void search()}
          >
            다시 시도
          </button>
        </section>
      ) : (
        <div className="organization-workspace">
          <section className="organization-list-panel">
            <div className="list-header">
              <h2>조직 목록</h2>
              <span>{organizations.length}건</span>
            </div>
            {state === "loading" ? (
              <div className="skeleton-line" />
            ) : organizations.length === 0 ? (
              <p className="empty-state">조직코드를 입력한 뒤 조회하세요.</p>
            ) : (
              <div className="table-wrap">
                <table>
                  <thead>
                    <tr>
                      <th>조직코드</th>
                      <th>조직명</th>
                      <th>조직 구분</th>
                      <th>현재 상위조직</th>
                      <th>처리</th>
                    </tr>
                  </thead>
                  <tbody>
                    {organizations.map((organization) => (
                      <tr
                        key={organization.organizationCode}
                        className={
                          selected?.organizationCode ===
                          organization.organizationCode
                            ? "selected-row"
                            : ""
                        }
                      >
                        <td>{organization.organizationCode}</td>
                        <td>{organization.organizationName}</td>
                        <td>{organization.organizationType}</td>
                        <td>
                          {organization.currentParentOrganizationCode ?? "-"}
                        </td>
                        <td>
                          <button
                            type="button"
                            className="text-button"
                            onClick={() => loadTree(organization)}
                          >
                            선택
                          </button>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
          </section>
          <aside className="organization-tree-panel">
            <p className="eyebrow">선택 조직 계층</p>
            {treeState === "loading" ? (
              <div className="skeleton-line" />
            ) : treeState === "error" ? (
              <div role="alert">
                <p>조직 계층을 불러올 수 없습니다.</p>
                {selected && (
                  <button
                    type="button"
                    className="secondary-button"
                    onClick={() => loadTree(selected)}
                  >
                    다시 시도
                  </button>
                )}
              </div>
            ) : tree ? (
              <>
                <h2>
                  {tree.organizationName} <span>{tree.organizationCode}</span>
                </h2>
                <div className="tree-relationship">
                  <p>
                    상위조직{" "}
                    <strong>{tree.parent?.organizationName ?? "없음"}</strong>
                  </p>
                  <p className="tree-current">└─ {tree.organizationName}</p>
                  {tree.children.length ? (
                    tree.children.map((child) => (
                      <p key={child.organizationCode}>
                        　└─ {child.organizationName}
                      </p>
                    ))
                  ) : (
                    <p className="tree-empty">하위조직 없음</p>
                  )}
                </div>
                <h3>관계 변경 이력</h3>
                <ul className="relation-history">
                  {tree.relationHistory.map((relation) => (
                    <li
                      key={`${relation.effectiveStartDate}-${relation.parentOrganizationCode ?? "root"}`}
                    >
                      {relation.parentOrganizationCode ?? "상위조직 없음"} ·{" "}
                      {relation.effectiveStartDate} ~{" "}
                      {relation.effectiveEndDate ?? "현재"}
                    </li>
                  ))}
                </ul>
                <button
                  type="button"
                  className="primary-button"
                  onClick={openRelationModal}
                >
                  관계·적용기간 변경
                </button>
              </>
            ) : (
              <p className="empty-state">목록에서 조직을 선택하세요.</p>
            )}
          </aside>
        </div>
      )}
      {modalOpen && selected && (
        <div className="modal-backdrop" role="presentation">
          <form
            className="settings-modal"
            aria-label="관계·적용기간 변경"
            role="dialog"
            onSubmit={saveRelation}
          >
            <div className="modal-heading">
              <div>
                <p className="eyebrow">
                  {selected.organizationName} · {selected.organizationCode}
                </p>
                <h2>관계·적용기간 변경</h2>
              </div>
              <button
                type="button"
                className="icon-button"
                aria-label="닫기"
                onClick={() => setModalOpen(false)}
              >
                ×
              </button>
            </div>
            <p className="source-notice">
              조직 원천 정보는 변경할 수 없습니다. 기존 유효 관계를 침범하는
              적용기간은 저장되지 않습니다.
            </p>
            <label>
              상위조직
              <input
                aria-label="상위조직"
                value={parentOrganizationCode}
                onChange={(event) =>
                  setParentOrganizationCode(event.target.value)
                }
              />
            </label>
            <label>
              적용 시작일
              <input
                aria-label="적용 시작일"
                type="date"
                value={effectiveStartDate}
                onChange={(event) => setEffectiveStartDate(event.target.value)}
                required
              />
            </label>
            <label>
              적용 종료일
              <input
                aria-label="적용 종료일"
                type="date"
                value={effectiveEndDate}
                onChange={(event) => setEffectiveEndDate(event.target.value)}
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
                onClick={() => setModalOpen(false)}
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
