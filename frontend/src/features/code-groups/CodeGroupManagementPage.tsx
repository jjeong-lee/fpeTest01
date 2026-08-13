import { FormEvent, useEffect, useState } from "react";
import { api, type ApiError } from "../../api/client";

type CodeGroup = {
  groupId: string;
  groupName: string;
  description: string | null;
  managingDepartment: string | null;
  useStatus: string;
};

type Form = {
  groupId: string;
  groupName: string;
  description: string;
  managingDepartment: string;
  reason: string;
};
const emptyForm: Form = {
  groupId: "",
  groupName: "",
  description: "",
  managingDepartment: "",
  reason: "",
};

export function CodeGroupManagementPage() {
  const [groups, setGroups] = useState<CodeGroup[]>([]);
  const [state, setState] = useState<
    "loading" | "idle" | "error" | "permission"
  >("loading");
  const [selected, setSelected] = useState<CodeGroup | null>(null);
  const [form, setForm] = useState<Form>(emptyForm);
  const [modalMode, setModalMode] = useState<"create" | "edit" | null>(null);
  const [formError, setFormError] = useState("");
  const [message, setMessage] = useState("");

  async function load(clearMessage = true) {
    setState("loading");
    if (clearMessage) setMessage("");
    try {
      const response = await api<CodeGroup[]>("/api/code-groups");
      setGroups(response.data);
      setSelected((current) =>
        current
          ? (response.data.find((group) => group.groupId === current.groupId) ??
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

  function openCreate() {
    setForm(emptyForm);
    setFormError("");
    setModalMode("create");
  }

  function openEdit() {
    if (!selected) return;
    setForm({
      groupId: selected.groupId,
      groupName: selected.groupName,
      description: selected.description ?? "",
      managingDepartment: selected.managingDepartment ?? "",
      reason: "",
    });
    setFormError("");
    setModalMode("edit");
  }

  function closeModal() {
    setModalMode(null);
    setForm(emptyForm);
    setFormError("");
  }

  function updateField(field: keyof Form, value: string) {
    setForm((current) => ({ ...current, [field]: value }));
  }

  async function save(event: FormEvent) {
    event.preventDefault();
    if (!form.groupId.trim()) return setFormError("그룹ID를 입력하세요.");
    if (!form.groupName.trim()) return setFormError("명칭을 입력하세요.");
    if (!form.reason.trim()) return setFormError("변경 사유를 입력하세요.");
    const payload = {
      ...form,
      groupId: form.groupId.trim(),
      groupName: form.groupName.trim(),
      reason: form.reason.trim(),
    };
    try {
      if (modalMode === "create")
        await api<CodeGroup>("/api/code-groups", {
          method: "POST",
          body: JSON.stringify(payload),
        });
      else
        await api<CodeGroup>(`/api/code-groups/${selected?.groupId}`, {
          method: "PUT",
          body: JSON.stringify(payload),
        });
      closeModal();
      setMessage("코드그룹이 저장되었습니다.");
      await load(false);
    } catch (error) {
      const apiError = error as ApiError;
      if (apiError.error?.code === "MENU_ACCESS_DENIED") setState("permission");
      else
        setFormError(
          apiError.error?.fieldErrors?.[0]?.message ??
            apiError.error?.message ??
            "코드그룹을 저장할 수 없습니다.",
        );
    }
  }

  return (
    <section
      className="code-group-management"
      aria-labelledby="code-group-management-title"
    >
      <div className="page-title-row">
        <div>
          <p className="eyebrow">시스템 관리 · 공통코드 관리</p>
          <h1 id="code-group-management-title">코드그룹 관리</h1>
          <p>코드그룹을 등록·수정하고 상세코드 관리로 이동합니다.</p>
        </div>
        <div className="page-actions">
          <button
            type="button"
            className="secondary-button"
            onClick={() => void load()}
          >
            새로고침
          </button>
          <button type="button" className="primary-button" onClick={openCreate}>
            코드그룹 등록
          </button>
        </div>
      </div>
      {message && (
        <p className="success-message" role="status">
          {message}
        </p>
      )}
      {state === "permission" ? (
        <section className="status-card permission-state" role="alert">
          <h2>코드그룹 관리 권한이 없습니다.</h2>
          <p>현재 계정에는 이 메뉴를 사용할 권한이 없습니다.</p>
        </section>
      ) : state === "error" ? (
        <section className="status-card" role="alert">
          <h2>코드그룹 목록을 불러올 수 없습니다.</h2>
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
          <section className="code-group-list-panel">
            <div className="list-header">
              <h2>코드그룹 목록</h2>
              <span>{groups.length}건</span>
            </div>
            {state === "loading" ? (
              <div className="skeleton-line" />
            ) : groups.length === 0 ? (
              <p className="empty-state">등록된 코드그룹이 없습니다.</p>
            ) : (
              <div className="table-wrap">
                <table>
                  <thead>
                    <tr>
                      <th>그룹ID</th>
                      <th>명칭</th>
                      <th>설명</th>
                      <th>관리부서</th>
                      <th>관리</th>
                    </tr>
                  </thead>
                  <tbody>
                    {groups.map((group) => (
                      <tr
                        key={group.groupId}
                        className={
                          selected?.groupId === group.groupId
                            ? "selected-row"
                            : ""
                        }
                      >
                        <td>{group.groupId}</td>
                        <td>{group.groupName}</td>
                        <td>{group.description || "-"}</td>
                        <td>{group.managingDepartment || "-"}</td>
                        <td>
                          <button
                            type="button"
                            className="text-button"
                            aria-label={`${group.groupName} 선택`}
                            onClick={() => setSelected(group)}
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
          <section className="code-group-selection-panel">
            {selected ? (
              <>
                <p className="eyebrow">선택한 코드그룹</p>
                <h2>
                  {selected.groupName} <span>{selected.groupId}</span>
                </h2>
                <p>{selected.description || "설명이 등록되지 않았습니다."}</p>
                <div className="modal-actions">
                  <button
                    type="button"
                    className="secondary-button"
                    onClick={openEdit}
                  >
                    수정
                  </button>
                  <a
                    className="primary-button"
                    href={`/system/detail-codes?groupId=${encodeURIComponent(selected.groupId)}`}
                  >
                    상세코드 목록
                  </a>
                </div>
              </>
            ) : (
              <p className="empty-state">목록에서 코드그룹을 선택하세요.</p>
            )}
          </section>
        </>
      )}
      {modalMode && (
        <div className="modal-backdrop" role="presentation">
          <form
            className="settings-modal"
            onSubmit={save}
            role="dialog"
            aria-modal="true"
            aria-labelledby="code-group-modal-title"
          >
            <div className="modal-heading">
              <div>
                <p className="eyebrow">공통코드 관리</p>
                <h2 id="code-group-modal-title">
                  {modalMode === "create" ? "코드그룹 등록" : "코드그룹 수정"}
                </h2>
              </div>
              <button
                type="button"
                className="icon-button"
                aria-label="닫기"
                onClick={closeModal}
              >
                ×
              </button>
            </div>
            <label>
              그룹ID
              <input
                aria-label="그룹ID"
                value={form.groupId}
                disabled={modalMode === "edit"}
                onChange={(event) => updateField("groupId", event.target.value)}
              />
            </label>
            <label>
              명칭
              <input
                aria-label="명칭"
                value={form.groupName}
                onChange={(event) =>
                  updateField("groupName", event.target.value)
                }
              />
            </label>
            <label>
              설명
              <textarea
                aria-label="설명"
                value={form.description}
                onChange={(event) =>
                  updateField("description", event.target.value)
                }
              />
            </label>
            <label>
              관리부서
              <input
                aria-label="관리부서"
                value={form.managingDepartment}
                onChange={(event) =>
                  updateField("managingDepartment", event.target.value)
                }
              />
              <span className="field-hint">
                관리부서 참조 형식은 확인 필요입니다.
              </span>
            </label>
            <label>
              변경 사유
              <textarea
                aria-label="변경 사유"
                value={form.reason}
                onChange={(event) => updateField("reason", event.target.value)}
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
                onClick={closeModal}
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
