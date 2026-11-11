import { useEffect, useState, type FormEvent } from 'react'
import RegisterForm from './RegisterForm'
import {
  ApiError,
  clearToken,
  getCurrentUser,
  getErrorMessage,
  getToken,
  loginUser,
  logoutUser,
  saveToken,
  type UserProfile,
} from './api'

export default function App() {
  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')
  const [message, setMessage] = useState('')
  const [submitting, setSubmitting] = useState(false)
  const [showRegister, setShowRegister] = useState(false)
  const [user, setUser] = useState<UserProfile | null>(null)
  const [restoring, setRestoring] = useState(() => Boolean(getToken()))
  const [restoreFailed, setRestoreFailed] = useState(false)
  const [restoreAttempt, setRestoreAttempt] = useState(0)

  // 页面加载后，用保存的令牌查询当前用户。
  useEffect(() => {
    const token = getToken()
    if (!token) return

    const controller = new AbortController()

    async function restoreSession() {
      try {
        const profile = await getCurrentUser(token!, controller.signal)
        if (controller.signal.aborted) return

        setUser(profile)
        setRestoreFailed(false)
        setMessage('')
      } catch (error) {
        if (controller.signal.aborted) return

        if (error instanceof ApiError && error.status === 401) {
          clearToken()
          setRestoreFailed(false)
          setMessage('登录状态已失效，请重新登录')
        } else {
          setRestoreFailed(true)
          setMessage(getErrorMessage(error))
        }
      } finally {
        if (!controller.signal.aborted) {
          setRestoring(false)
        }
      }
    }

    void restoreSession()
    return () => controller.abort()
  }, [restoreAttempt])

  async function handleLogin(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setMessage('')
    setSubmitting(true)

    try {
      const login = await loginUser(username.trim(), password)
      const profile = await getCurrentUser(login.accessToken)

      saveToken(login.accessToken)
      setUser(profile)
      setPassword('')
    } catch (error) {
      setMessage(getErrorMessage(error))
    } finally {
      setSubmitting(false)
    }
  }

  async function handleLogout() {
    setMessage('')
    setSubmitting(true)

    try {
      const token = getToken()

      if (token) {
        try {
          await logoutUser(token)
        } catch (error) {
          // 已失效或已撤销的令牌也可以完成本地清理。
          if (!(error instanceof ApiError && error.status === 401)) {
            throw error
          }
        }
      }

      clearToken()
      setUser(null)
      setPassword('')
      setShowRegister(false)
      setMessage('已退出登录')
    } catch (error) {
      setMessage(getErrorMessage(error))
    } finally {
      setSubmitting(false)
    }
  }

  if (restoring) {
    return (
      <main>
        <h1>PetCure</h1>
        <p role="status">正在检查登录状态…</p>
      </main>
    )
  }

  if (restoreFailed) {
    return (
      <main>
        <h1>PetCure</h1>
        <p role="status">{message}</p>
        <button
          type="button"
          onClick={() => {
            setRestoring(true)
            setRestoreAttempt((attempt) => attempt + 1)
          }}
        >
          重试
        </button>
      </main>
    )
  }

  if (user) {
    return (
      <main>
        <h1>PetCure</h1>
        <p>欢迎，{user.nickname || user.username}</p>
        <p>账号：{user.username}</p>
        <button
          type="button"
          onClick={handleLogout}
          disabled={submitting}
        >
          {submitting ? '退出中…' : '退出登录'}
        </button>
        <p role="status">{message}</p>
      </main>
    )
  }

  if (showRegister) {
    return (
      <RegisterForm
        onBack={() => setShowRegister(false)}
        onRegistered={(registeredUsername) => {
          setUsername(registeredUsername)
          setPassword('')
          setMessage('注册成功，请登录')
          setShowRegister(false)
        }}
      />
    )
  }

  return (
    <main>
      <h1>PetCure 登录</h1>

      <form onSubmit={handleLogin}>
        <fieldset disabled={submitting}>
          <legend>登录信息</legend>

          <p>
            <label>
              用户名<br />
              <input
                autoComplete="username"
                value={username}
                onChange={(event) => setUsername(event.target.value)}
                maxLength={50}
                required
              />
            </label>
          </p>

          <p>
            <label>
              密码<br />
              <input
                type="password"
                autoComplete="current-password"
                value={password}
                onChange={(event) => setPassword(event.target.value)}
                maxLength={64}
                required
              />
            </label>
          </p>

          <button type="submit">
            {submitting ? '登录中…' : '登录'}
          </button>
          <button
            type="button"
            onClick={() => {
              setMessage('')
              setShowRegister(true)
            }}
          >
            去注册
          </button>
        </fieldset>
      </form>

      <p role="status">{message}</p>
    </main>
  )
}