export type UserProfile = {
    id:number
    username:string
    nickname:string
    avatarUrl:string|null
}

type LoginResponse = {
    accessToken:string
    tokenType: string
    expiresAt:string
}

export type RegisterRequest = {
    username:string
    password:string
    confirmPassword:string
    phone:string | null
    email:string | null
}

export class ApiError extends Error {
  public readonly status: number

  constructor(message: string, status: number) {
    super(message)
    this.name = 'ApiError'
    this.status = status
  }
}

const TOKEN_KEY = 'petcure.accessToken'

export function getToken() {
  return sessionStorage.getItem(TOKEN_KEY)
}

export function saveToken(token: string) {
  sessionStorage.setItem(TOKEN_KEY, token)
}

export function clearToken() {
  sessionStorage.removeItem(TOKEN_KEY)
}

async function request<T>(
  path: string,
  options: RequestInit = {},
): Promise<T> {
  const response = await fetch(path, options)
  const result = await response.json().catch(() => null)

  if (!response.ok || result?.code !== 'SUCCESS') {
    throw new ApiError(
      result?.message || `请求失败（HTTP ${response.status}）`,
      response.status,
    )
  }

  return result.data as T
}

export function registerUser(data: RegisterRequest) {
  return request<null>('/api/auth/register', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(data),
  })
}

export async function loginUser(username: string, password: string) {
  const data = await request<LoginResponse>('/api/auth/login', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ username, password }),
  })

  if (!data || typeof data.accessToken !== 'string' || !data.accessToken) {
    throw new Error('登录响应缺少访问令牌')
  }

  return data
}

export function getCurrentUser(token: string, signal?: AbortSignal) {
  return request<UserProfile>('/api/users/me', {
    headers: { Authorization: `Bearer ${token}` },
    signal,
  })
}

export function logoutUser(token: string) {
  return request<null>('/api/auth/logout', {
    method: 'POST',
    headers: { Authorization: `Bearer ${token}` },
  })
}

export function getErrorMessage(error: unknown) {
  if (error instanceof ApiError) return error.message
  return '请求未能完成，请检查连接后重试'
}