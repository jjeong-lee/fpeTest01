import { FormEvent, useEffect, useState } from "react";
import { api, type ApiError } from "../../api/client";

type CodeGroup = {
  groupId: string;
  groupName: string;
  description: string | null;
  managingDepartment: string | null;
  useStatus: string;
};
type DetailCode = {
  detailCodeId: string;
  groupId: string;
  codeValue: string;
  codeName: string;
  parentDetailCodeId: string | null;
  displayOrder: number;
  additionalAttributes: Record<string, unknown>;
  useStatus: string;
};
type Form = {
  codeValue: string;
  codeName: string;
  parentDetailCodeId: string;
  displayOrder: string;
  additionalAttributes: string;
  reason: string;
};
const emptyForm: Form = {
  codeValue: "",
  codeName: "",
  parentDetailCodeId: "",
  displayOrder: "0",
  additionalAttributes: "{}",
  reason: "",
};

type Props = { initialGroupId?: string };

export function DetailCodeManagementPage({ initialGroupId }: Props) {
  const [groups, setGroups] = useState<CodeGroup[]>([]);
  const [groupId, setGroupId] = useState(initialGroupId ?? "");
  const [codes, setCodes] = useState<DetailCode[]>([]);
  const [state, setState] = useState<
    "loading" | "idle" | "error" | "permission" | "unselected"
  >("loading");
  const [selected, setSelected] = useState<DetailCode | null>(null);
  const [modalMode, setModalMode] = useState<"create" | "edit" | null>(null);
  const [form, setForm] = useState<Form>(emptyForm);
  const [formError, setFormError] = useState("");
  const [message, setMessage] = useState("");

  async function loadGroups() {
    try {
      const response = await api<CodeGroup[]>("/api/code-groups");
      setGroups(response.data);
      if (!groupId && response.data.length === 1)
        setGroupId(response.data[0].groupId);
    } catch (error) {
      setState(
        (error as ApiError).error?.code === "MENU_ACCESS_DENIED"
          ? "permission"
          : "error",
      );
    }
  }

  async function loadCodes(targetGroupId = groupId, clearMessage = true) {
    if (!targetGroupId) {
      setCodes([]);
      setSelected(null);
      setState("unselected");
      return;
    }
    setState("loading");
    if (clearMessage) setMessage("");
    try {
      const response = await api<DetailCode[]>(
        `/api/code-groups/${encodeURIComponent(targetGroupId)}/detail-codes`,
      );
      setCodes(response.data);
      setSelected((current) =>
        current
          ? (response.data.find(
              (code) => code.detailCodeId === current.detailCodeId,
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
    void loadGroups();
  }, []);
  useEffect(() => {
    if (initialGroupId) void loadCodes(initialGroupId);
  }, [initialGroupId]);

  function updateField(field: keyof Form, value: string) {
    setForm((current) => ({ ...current, [field]: value }));
  }
  function openCreate() {
    setForm(emptyForm);
    setFormError("");
    setModalMode("create");
  }
  function openEdit() {
    if (!selected) return;
    setForm({
      codeValue: selected.codeValue,
      codeName: selected.codeName,
      parentDetailCodeId: selected.parentDetailCodeId ?? "",
      displayOrder: String(selected.displayOrder),
      additionalAttributes: JSON.stringify(selected.additionalAttributes),
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

  async function save(event: FormEvent) {
    event.preventDefault();
    if (!groupId) return setFormError("코드그룹을 선택하세요.");
    if (!form.codeValue.trim()) return setFormError("코드값을 입력하세요.");
    if (!form.codeName.trim()) return setFormError("코드명을 입력하세요.");
    if (!form.reason.trim()) return setFormError("변경 사유를 입력하세요.");
    const displayOrder = Number(form.displayOrder);
    if (!Number.isInteger(displayOrder) || displayOrder < 0)
      return setFormError("정렬순서는 0 이상의 정수여야 합니다.");
    let additionalAttributes: Record<string, unknown>;
    try {
      const parsed = JSON.parse(form.additionalAttributes || "{}");
      if (!parsed || Array.isArray(parsed) || typeof parsed !== "object")
        throw new Error();
      additionalAttributes = parsed as Record<string, unknown>;
    } catch {
      return setFormError("추가속성은 JSON 객체 형식이어야 합니다.");
    }
    const payload = {
      groupId,
      codeValue: form.codeValue.trim(),
      codeName: form.codeName.trim(),
      parentDetailCodeId: form.parentDetailCodeId || null,
      displayOrder,
      additionalAttributes,
      reason: form.reason.trim(),
    };
    try {
      if (modalMode === "create")
        await api<DetailCode>("/api/detail-codes", {
          method: "POST",
          body: JSON.stringify(payload),
        });
      else if (selected)
        await api<DetailCode>(`/api/detail-codes/${selected.detailCodeId}`, {
          method: "PUT",
          body: JSON.stringify(payload),
        });
      closeModal();
      setMessage("상세코드가 저장되었습니다.");
      await loadCodes(groupId, false);
    } catch (error) {
      const apiError = error as ApiError;
      if (apiError.error?.code === "MENU_ACCESS_DENIED") setState("permission");
      else
        setFormError(
          apiError.error?.fieldErrors?.[0]?.message ??
            apiError.error?.message ??
            "상세코드를 저장할 수 없습니다.",
        );
    }
  }

  const parentName = (parentDetailCodeId: string | null) =>
    codes.find((code) => code.detailCodeId === parentDetailCodeId)?.codeValue ??
    "-";
  const attributeText = (attributes: Record<string, unknown>) =>
    Object.values(attributes).map(String).join(", ") || "-";

  return (
    <section
      className="detail-code-management"
      aria-labelledby="detail-code-management-title"
    >
      <div className="page-title-row">
        <div>
          <p className="eyebrow">시스템 관리 · 공통코드 관리</p>
          <h1 id="detail-code-management-title">상세코드 관리</h1>
          <p>선택한 코드그룹의 코드 계층과 연계 속성을 관리합니다.</p>
        </div>
        <div className="page-actions">
          <button
            type="button"
            className="secondary-button"
            onClick={() => void loadCodes()}
          >
            새로고침
          </button>
          <button
            type="button"
            className="primary-button"
            disabled={!groupId || state === "permission"}
            onClick={openCreate}
          >
            상세코드 등록
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
          <h2>상세코드 관리 권한이 없습니다.</h2>
          <p>현재 계정에는 이 메뉴를 사용할 권한이 없습니다.</p>
        </section>
      ) : (
        <>
          <section className="detail-code-filter">
            <label>
              코드그룹
              <select
                aria-label="코드그룹"
                value={groupId}
                onChange={(event) => {
                  setGroupId(event.target.value);
                  setSelected(null);
                  setCodes([]);
                  setState("unselected");
                }}
              >
                <option value="">코드그룹 선택</option>
                {groups.map((group) => (
                  <option key={group.groupId} value={group.groupId}>
                    {group.groupName} ({group.groupId})
                  </option>
                ))}
              </select>
            </label>
            <button
              type="button"
              className="primary-button"
              disabled={!groupId}
              onClick={() => void loadCodes()}
            >
              조회
            </button>
          </section>
          {state === "unselected" ? (
            <p className="empty-state">코드그룹을 선택하세요.</p>
          ) : state === "error" ? (
            <section className="status-card" role="alert">
              <h2>상세코드 목록을 불러올 수 없습니다.</h2>
              <button
                type="button"
                className="secondary-button"
                onClick={() => void loadCodes()}
              >
                다시 시도
              </button>
            </section>
          ) : (
            <>
              <section className="detail-code-list-panel">
                <div className="list-header">
                  <h2>상세코드 목록</h2>
                  <span>{codes.length}건</span>
                </div>
                {state === "loading" ? (
                  <div className="skeleton-line" />
                ) : codes.length === 0 ? (
                  <p className="empty-state">등록된 상세코드가 없습니다.</p>
                ) : (
                  <div className="table-wrap">
                    <table>
                      <thead>
                        <tr>
                          <th>코드값</th>
                          <th>코드명</th>
                          <th>상위코드</th>
                          <th>정렬순서</th>
                          <th>추가속성</th>
                          <th>관리</th>
                        </tr>
                      </thead>
                      <tbody>
                        {codes.map((code) => (
                          <tr
                            key={code.detailCodeId}
                            className={
                              selected?.detailCodeId === code.detailCodeId
                                ? "selected-row"
                                : ""
                            }
                          >
                            <td>{code.codeValue}</td>
                            <td>{code.codeName}</td>
                            <td>{parentName(code.parentDetailCodeId)}</td>
                            <td>{code.displayOrder}</td>
                            <td>{attributeText(code.additionalAttributes)}</td>
                            <td>
                              <button
                                type="button"
                                className="text-button"
                                aria-label={`${code.codeName} 선택`}
                                onClick={() => setSelected(code)}
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
              <section className="detail-code-selection-panel">
                {selected ? (
                  <>
                    <p className="eyebrow">선택한 상세코드</p>
                    <h2>
                      {selected.codeName} <span>{selected.codeValue}</span>
                    </h2>
                    <p>
                      상위코드: {parentName(selected.parentDetailCodeId)} ·
                      추가속성은 연계 코드 매핑 정책 확인이 필요합니다.
                    </p>
                    <button
                      type="button"
                      className="secondary-button"
                      onClick={openEdit}
                    >
                      수정
                    </button>
                  </>
                ) : (
                  <p className="empty-state">목록에서 상세코드를 선택하세요.</p>
                )}
              </section>
            </>
          )}
        </>
      )}
      {modalMode && (
        <div className="modal-backdrop" role="presentation">
          <form
            className="settings-modal"
            onSubmit={save}
            role="dialog"
            aria-modal="true"
            aria-labelledby="detail-code-modal-title"
          >
            <div className="modal-heading">
              <div>
                <p className="eyebrow">공통코드 관리</p>
                <h2 id="detail-code-modal-title">
                  {modalMode === "create" ? "상세코드 등록" : "상세코드 수정"}
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
              코드그룹
              <input aria-label="저장 대상 코드그룹" value={groupId} disabled />
            </label>
            <label>
              코드값
              <input
                aria-label="코드값"
                value={form.codeValue}
                onChange={(event) =>
                  updateField("codeValue", event.target.value)
                }
              />
            </label>
            <label>
              코드명
              <input
                aria-label="코드명"
                value={form.codeName}
                onChange={(event) =>
                  updateField("codeName", event.target.value)
                }
              />
            </label>
            <label>
              상위코드
              <select
                aria-label="상위코드"
                value={form.parentDetailCodeId}
                onChange={(event) =>
                  updateField("parentDetailCodeId", event.target.value)
                }
              >
                <option value="">상위코드 없음</option>
                {codes
                  .filter(
                    (code) => code.detailCodeId !== selected?.detailCodeId,
                  )
                  .map((code) => (
                    <option key={code.detailCodeId} value={code.detailCodeId}>
                      {code.codeValue} · {code.codeName}
                    </option>
                  ))}
              </select>
            </label>
            <label>
              정렬순서
              <input
                aria-label="정렬순서"
                type="number"
                min="0"
                value={form.displayOrder}
                onChange={(event) =>
                  updateField("displayOrder", event.target.value)
                }
              />
            </label>
            <label>
              추가속성
              <textarea
                aria-label="추가속성"
                value={form.additionalAttributes}
                onChange={(event) =>
                  updateField("additionalAttributes", event.target.value)
                }
              />
              <span className="field-hint">
                연계 코드 매핑용 추가속성 구조는 확인 필요입니다.
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
