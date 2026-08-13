import { useState, type FormEvent } from "react";
import { api, type ApiError } from "../api/client";

type CurrentUser = {
  username: string;
  roles: string[];
  allowedMenuPaths: string[];
};

type Props = {
  onAuthenticated: (user: CurrentUser) => void;
};

export function LoginPage({ onAuthenticated }: Props) {
  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState<string>();
  const [submitting, setSubmitting] = useState(false);

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setSubmitting(true);
    setError(undefined);
    try {
      const { data } = await api<CurrentUser>("/api/auth/login", {
        method: "POST",
        body: JSON.stringify({ username, password }),
      });
      onAuthenticated(data);
    } catch (unknownError: unknown) {
      const apiError = unknownError as ApiError;
      setError(
        apiError.error?.code === "INVALID_CREDENTIALS"
          ? "사용자명 또는 비밀번호가 올바르지 않습니다."
          : "로그인 요청을 처리할 수 없습니다. 잠시 후 다시 시도하세요.",
      );
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <main className="page-canvas">
      <div className="frame">
        <section className="status-card login-card" aria-labelledby="login-title">
          <p className="eyebrow">교수업적평가시스템</p>
          <h1 id="login-title">로그인</h1>
          <p>시스템을 이용하려면 사용자명과 비밀번호를 입력하세요.</p>
          <form className="login-form" onSubmit={submit}>
            <label>
              사용자명
              <input
                autoComplete="username"
                disabled={submitting}
                onChange={(event) => setUsername(event.target.value)}
                required
                value={username}
              />
            </label>
            <label>
              비밀번호
              <input
                autoComplete="current-password"
                disabled={submitting}
                onChange={(event) => setPassword(event.target.value)}
                required
                type="password"
                value={password}
              />
            </label>
            {error ? <p className="form-error" role="alert">{error}</p> : null}
            <button className="primary-button" disabled={submitting} type="submit">
              {submitting ? "로그인 중..." : "로그인"}
            </button>
          </form>
        </section>
      </div>
    </main>
  );
}
