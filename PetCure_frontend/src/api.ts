export type UserProfile = {
    id:number
    username:string
    nickname:string|null
    avatarUrl:string|null
    gender:number
    birthday:string|null
    age:number|null
    occupation:string|null
    city:string|null
    bio:string|null
    phone:string|null
    email:string|null
    createdAt:string|null
    lastLoginAt?:string|null
    LastLoginAt?:string|null
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

export type ProfileUpdate = Pick<UserProfile,
  'nickname' | 'gender' | 'birthday' | 'occupation' | 'city' | 'bio'>
export type ContactUpdate = { currentPassword: string; phone: string | null; email: string | null }
export type PasswordUpdate = { currentPassword: string; newPassword: string; confirmPassword: string }
type FileUploadResponse = { fileId: number; mimeType: string; sizeBytes: number }

function jsonPut(token: string, data: unknown): RequestInit {
  return {
    method: 'PUT',
    headers: { Authorization: `Bearer ${token}`, 'Content-Type': 'application/json' },
    body: JSON.stringify(data),
  }
}

export function updateUserProfile(token: string, data: ProfileUpdate) {
  return request<UserProfile>('/api/users/me/profile', jsonPut(token, data))
}

export function updateUserContact(token: string, data: ContactUpdate) {
  return request<UserProfile>('/api/users/me/contact', jsonPut(token, data))
}

export function updateUserPassword(token: string, data: PasswordUpdate) {
  return request<null>('/api/users/me/password', jsonPut(token, data))
}

export function uploadAvatar(token: string, file: File) {
  const body = new FormData()
  body.append('file', file)
  return request<FileUploadResponse>('/api/files/avatar', {
    method: 'POST', headers: { Authorization: `Bearer ${token}` }, body,
  })
}

export function setUserAvatar(token: string, fileId: number) {
  return request<UserProfile>('/api/users/me/avatar', jsonPut(token, { fileId }))
}

export async function getAvatarBlob(token: string, path: string, signal?: AbortSignal) {
  if (!/^\/api\/files\/[1-9]\d*\/content$/.test(path)) {
    throw new ApiError('头像地址无效', 400)
  }
  const response = await fetch(path, {
    headers: { Authorization: `Bearer ${token}` }, cache: 'no-store', signal,
  })
  if (!response.ok) {
    const result = await response.json().catch(() => null)
    throw new ApiError(result?.message || '头像读取失败', response.status)
  }
  const mime = response.headers.get('Content-Type')?.split(';')[0].trim().toLowerCase()
  if (!mime || !['image/jpeg', 'image/png', 'image/webp'].includes(mime)) {
    throw new ApiError('后端没有返回有效的头像图片', response.status)
  }
  return response.blob()
}
