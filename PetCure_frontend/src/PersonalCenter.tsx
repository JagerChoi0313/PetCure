import { useEffect, useRef, useState, type FormEvent } from 'react'
import {
  ApiError, getAvatarBlob, getCurrentUser, getErrorMessage, getToken,
  setUserAvatar, updateUserContact, updateUserPassword, updateUserProfile,
  uploadAvatar, type UserProfile,
} from './api'

type Props = {
  user: UserProfile
  onUserChange: (user: UserProfile) => void
  onSessionEnded: (message: string) => void
  onLogout: () => Promise<void>
  loggingOut: boolean
  logoutMessage: string
}

function optionalText(data: FormData, name: string) {
  return String(data.get(name) ?? '').trim() || null
}

function password(data: FormData, name: string) {
  return String(data.get(name) ?? '')
}

function clearPasswords(form: HTMLFormElement) {
  form.querySelectorAll<HTMLInputElement>('input[type="password"]').forEach((input) => {
    input.value = ''
  })
}

function formatTime(value?: string | null) {
  if (!value) return '未填写'
  // 后端时间使用 UTC，但 LocalDateTime 的 JSON 没有时区后缀。
  const date = new Date(/(?:Z|[+-]\d{2}:\d{2})$/i.test(value) ? value : `${value}Z`)
  return Number.isNaN(date.getTime()) ? value : date.toLocaleString('zh-CN', {
    timeZone: 'Asia/Shanghai', hour12: false,
  })
}

function Avatar({ path, onSessionEnded }: {
  path: string | null
  onSessionEnded: Props['onSessionEnded']
}) {
  const [source, setSource] = useState('')
  const [error, setError] = useState('')

  useEffect(() => {
    if (!path) return
    const token = getToken()
    if (!token) {
      onSessionEnded('请重新登录')
      return
    }
    const controller = new AbortController()
    let objectUrl = ''
    // img 标签无法直接附加 Bearer 令牌，先获取图片，再创建本地预览地址。
    void getAvatarBlob(token, path, controller.signal).then((blob) => {
      if (controller.signal.aborted || getToken() !== token) return
      objectUrl = URL.createObjectURL(blob)
      setSource(objectUrl)
    }).catch((reason: unknown) => {
      if (controller.signal.aborted || getToken() !== token) return
      if (reason instanceof ApiError && reason.status === 401) {
        onSessionEnded('登录状态已失效，请重新登录')
      } else {
        setError(getErrorMessage(reason))
      }
    })
    return () => {
      controller.abort()
      if (objectUrl) URL.revokeObjectURL(objectUrl)
    }
  }, [path, onSessionEnded])

  if (!path) return <p>尚未设置头像</p>
  if (error) return <p role="alert">{error}，可以点击“刷新资料”重试。</p>
  if (!source) return <p>正在读取头像…</p>
  return <img src={source} alt="当前头像" width={96} height={96}
    style={{ objectFit: 'cover' }} onError={() => setError('头像无法显示')} />
}

export default function PersonalCenter({
  user, onUserChange, onSessionEnded, onLogout, loggingOut, logoutMessage,
}: Props) {
  const [busy, setBusy] = useState(false)
  const [message, setMessage] = useState('')
  const [uploadedFileId, setUploadedFileId] = useState<number | null>(null)
  const [avatarAttempt, setAvatarAttempt] = useState(0)
  const pending = useRef(false)
  const disabled = busy || loggingOut

  async function run(task: (token: string) => Promise<string>) {
    if (pending.current || loggingOut) return
    const token = getToken()
    if (!token) {
      onSessionEnded('请重新登录')
      return
    }
    pending.current = true
    setBusy(true)
    setMessage('')
    try {
      const notice = await task(token)
      if (getToken() === token) setMessage(notice)
    } catch (error) {
      if (getToken() !== token) return
      if (error instanceof ApiError && error.status === 401) {
        onSessionEnded('登录状态已失效，请重新登录')
      } else {
        setMessage(getErrorMessage(error))
      }
    } finally {
      pending.current = false
      setBusy(false)
    }
  }

  function applyUser(profile: UserProfile, token: string) {
    if (getToken() === token) onUserChange(profile)
  }

  function handleProfile(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    const data = new FormData(event.currentTarget)
    void run(async (token) => {
      const profile = await updateUserProfile(token, {
        nickname: optionalText(data, 'nickname'), gender: Number(data.get('gender')),
        birthday: optionalText(data, 'birthday'), occupation: optionalText(data, 'occupation'),
        city: optionalText(data, 'city'), bio: optionalText(data, 'bio'),
      })
      applyUser(profile, token)
      return '基本资料已保存'
    })
  }

  function handleContact(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    const form = event.currentTarget
    const data = new FormData(form)
    const currentPassword = password(data, 'currentPassword')
    const phone = optionalText(data, 'phone')
    const email = optionalText(data, 'email')
    if (!phone && !email) {
      setMessage('手机号和邮箱至少填写一项')
      clearPasswords(form)
      return
    }
    if (new TextEncoder().encode(currentPassword).length > 72) {
      setMessage('密码的 UTF-8 长度不能超过 72 字节')
      clearPasswords(form)
      return
    }
    void run(async (token) => {
      try {
        applyUser(await updateUserContact(token, { currentPassword, phone, email }), token)
        return '联系方式已保存'
      } finally {
        clearPasswords(form)
      }
    })
  }

  function handlePassword(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    const form = event.currentTarget
    const data = new FormData(form)
    const values = {
      currentPassword: password(data, 'currentPassword'),
      newPassword: password(data, 'newPassword'),
      confirmPassword: password(data, 'confirmPassword'),
    }
    let error = ''
    if (values.newPassword !== values.confirmPassword) error = '两次新密码不一致'
    else if (values.newPassword === values.currentPassword) error = '新密码不能与当前密码相同'
    else if (Object.values(values).some((value) => new TextEncoder().encode(value).length > 72)) {
      error = '密码的 UTF-8 长度不能超过 72 字节'
    }
    if (error) {
      setMessage(error)
      clearPasswords(form)
      return
    }
    void run(async (token) => {
      try {
        await updateUserPassword(token, values)
        if (getToken() === token) onSessionEnded('密码修改成功，请使用新密码重新登录')
        return ''
      } finally {
        clearPasswords(form)
      }
    })
  }

  function handleUpload(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    const file = new FormData(event.currentTarget).get('file')
    if (!(file instanceof File) || !file.size) {
      setMessage('请选择图片')
      return
    }
    if (file.size > 5 * 1024 * 1024) {
      setMessage('头像大小不能超过 5 MB')
      return
    }
    void run(async (token) => {
      const result = await uploadAvatar(token, file)
      if (getToken() === token) setUploadedFileId(result.fileId)
      return '图片上传成功，请点击“设为头像”'
    })
  }

  function handleBindAvatar() {
    if (uploadedFileId === null) return
    void run(async (token) => {
      applyUser(await setUserAvatar(token, uploadedFileId), token)
      if (getToken() === token) {
        setUploadedFileId(null)
        setAvatarAttempt((value) => value + 1)
      }
      return '头像已更新'
    })
  }

  return (
    <main style={{ maxWidth: 760, margin: '24px auto', padding: '0 16px' }}>
      <h1>PetCure 个人中心</h1>
      <p>欢迎，{user.nickname || user.username}</p>
      <button type="button" disabled={disabled} onClick={() => void run(async (token) => {
        applyUser(await getCurrentUser(token), token)
        if (getToken() === token) setAvatarAttempt((value) => value + 1)
        return '资料已刷新'
      })}>刷新资料</button>{' '}
      <button type="button" disabled={disabled} onClick={() => void onLogout()}>
        {loggingOut ? '退出中…' : '退出登录'}
      </button>
      <p role="status" aria-live="polite">{busy ? '正在处理…' : message || logoutMessage}</p>

      <section aria-labelledby="account-title">
        <h2 id="account-title">账号信息</h2>
        <p>账号：{user.username}</p>
        <p>年龄：{user.age == null ? '未填写' : `${user.age} 岁`}</p>
        <p>注册时间：{formatTime(user.createdAt)}</p>
        <p>最近登录：{formatTime(user.lastLoginAt ?? user.LastLoginAt)}</p>
      </section>

      <section aria-labelledby="avatar-title">
        <h2 id="avatar-title">头像</h2>
        <Avatar key={`${user.avatarUrl}:${avatarAttempt}`} path={user.avatarUrl}
          onSessionEnded={onSessionEnded} />
        <form onSubmit={handleUpload}>
          <fieldset disabled={disabled}>
            <legend>上传头像</legend>
            <p><label>选择图片 <input name="file" type="file" accept="image/jpeg,image/png,image/webp"
              required onChange={() => setUploadedFileId(null)} /></label></p>
            <p>支持 JPG、PNG、WebP，最大 5 MB，最多 2000 万像素。</p>
            <button type="submit">上传图片</button>
            {uploadedFileId !== null && <p>图片已上传（编号 {uploadedFileId}）。{' '}
              <button type="button" onClick={handleBindAvatar}>设为头像</button></p>}
          </fieldset>
        </form>
      </section>

      <section aria-labelledby="profile-title">
        <h2 id="profile-title">基本资料</h2>
        <form key={JSON.stringify([user.nickname, user.gender, user.birthday, user.occupation, user.city, user.bio])}
          onSubmit={handleProfile}>
          <fieldset disabled={disabled}>
            <legend>编辑基本资料</legend>
            <p><label>昵称 <input name="nickname" defaultValue={user.nickname ?? ''} maxLength={50} /></label></p>
            <p><label>性别 <select name="gender" defaultValue={user.gender ?? 0}>
              <option value={0}>未填写</option><option value={1}>男</option><option value={2}>女</option>
            </select></label></p>
            <p><label>生日 <input name="birthday" type="date" defaultValue={user.birthday ?? ''} /></label></p>
            <p><label>职业 <input name="occupation" defaultValue={user.occupation ?? ''} maxLength={100} /></label></p>
            <p><label>城市 <input name="city" defaultValue={user.city ?? ''} maxLength={100} /></label></p>
            <p><label>个人简介<br /><textarea name="bio" defaultValue={user.bio ?? ''} maxLength={500} rows={4} /></label></p>
            <button type="submit">保存基本资料</button>
          </fieldset>
        </form>
      </section>

      <section aria-labelledby="contact-title">
        <h2 id="contact-title">联系方式</h2>
        <form key={JSON.stringify([user.phone, user.email])} onSubmit={handleContact}>
          <fieldset disabled={disabled}>
            <legend>修改联系方式</legend>
            <p>手机号和邮箱至少保留一项。保存时会一起更新这两个字段。</p>
            <p><label>手机号 <input name="phone" type="tel" defaultValue={user.phone ?? ''}
              pattern={'\\+[1-9][0-9]{6,14}'} title="请使用带国家区号的手机号，如 +8613800138000" /></label></p>
            <p><label>邮箱 <input name="email" type="email" defaultValue={user.email ?? ''} maxLength={254} /></label></p>
            <p><label>当前密码 <input name="currentPassword" type="password" autoComplete="current-password" maxLength={64} required /></label></p>
            <button type="submit">保存联系方式</button>
          </fieldset>
        </form>
      </section>

      <section aria-labelledby="password-title">
        <h2 id="password-title">修改密码</h2>
        <form onSubmit={handlePassword}>
          <fieldset disabled={disabled}>
            <legend>修改登录密码</legend>
            <p><label>当前密码 <input name="currentPassword" type="password" autoComplete="current-password" maxLength={64} required /></label></p>
            <p><label>新密码 <input name="newPassword" type="password" autoComplete="new-password"
              minLength={8} maxLength={64} pattern="(?=.*[A-Za-z])(?=.*[0-9]).*"
              title="8～64 个字符，包含字母和数字，UTF-8 长度不超过 72 字节" required /></label></p>
            <p><label>确认新密码 <input name="confirmPassword" type="password" autoComplete="new-password" minLength={8} maxLength={64} required /></label></p>
            <p>修改成功后需要重新登录，其他设备上的旧登录凭证也会失效。</p>
            <button type="submit">修改密码</button>
          </fieldset>
        </form>
      </section>
    </main>
  )
}
