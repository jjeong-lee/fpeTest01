export type ApiResponse<T> = {
  success: true;
  data: T;
  meta: Record<string, unknown>;
};
export type ApiError = {
  success: false;
  error: {
    code: string;
    message: string;
    fieldErrors: Array<{ field: string; message: string }>;
  };
  meta: Record<string, unknown>;
};

export async function api<T>(
  path: string,
  options: RequestInit = {},
): Promise<ApiResponse<T>> {
  const response = await fetch(path, {
    credentials: "include",
    headers: { "Content-Type": "application/json", ...options.headers },
    ...options,
  });
  const body = (await response.json()) as ApiResponse<T> | ApiError;
  if (!response.ok) {
    if (response.status === 401 || response.status === 403) {
      const denied = body as ApiError;
      throw {
        ...denied,
        error: { ...denied.error, code: "MENU_ACCESS_DENIED" },
      } satisfies ApiError;
    }
    throw body;
  }
  return body as ApiResponse<T>;
}
