import './personal-center.css'
import { Paw, ProfileIcon } from './ProfileIcons'
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

  if (!path) return <div className="pc-avatar-placeholder" aria-label="尚未设置头像"><Paw /><span>尚未设置头像</span></div>
  if (error) return <p role="alert">{error}，可以点击“刷新资料”重试。</p>
  if (!source) return <p>正在读取头像…</p>
  return <img src={source} alt="当前头像" width={96} height={96}
    style={{ objectFit: 'cover' }} onError={() => setError('头像无法显示')} />
}

export default function PersonalCenter({
  user, onUserChange, onSessionEnded, onLogout, loggingOut, logoutMessage,
}: Props) {
  // 仅控制资料展示页和编辑页的切换，不改变资料提交逻辑。
  const [showDetails, setShowDetails] = useState(false)
  const [busy, setBusy] = useState(false)
  const [message, setMessage] = useState('')
  const [uploadedFileId, setUploadedFileId] = useState<number | null>(null)
  const [avatarAttempt, setAvatarAttempt] = useState(0)
  const pending = useRef(false)
  const disabled = busy || loggingOut

  // 切换资料页面时回到顶部，避免沿用长表单的滚动位置。
  useEffect(() => {
    window.scrollTo({ top: 0, behavior: 'instant' })
  }, [showDetails])

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
    <div className="pc-page">
      <header className="pc-header">
        <div className="pc-header-inner">
          <div className="pc-logo"><Paw /><span>PetCure</span></div>
          <span className="pc-current">个人中心</span>
          <button className="pc-logout" type="button" disabled={disabled} onClick={() => void onLogout()}>
            <ProfileIcon name="logout" />{loggingOut ? '退出中…' : '退出登录'}
          </button>
        </div>
      </header>
      <main className="pc-main">
        <nav className="pc-breadcrumb" aria-label="当前位置">
          {showDetails ? <><button type="button" disabled={disabled} onClick={() => setShowDetails(false)}>个人中心</button><span>/</span><span>详细资料</span></> : <span>个人中心</span>}
        </nav>
        <div className="pc-page-heading">
          <div><h1>{showDetails ? '详细资料' : '我的个人中心'}<span aria-hidden="true">〃</span></h1>
            <p>{showDetails ? '管理个人资料与账号信息。' : '关于我，还有与它一起的日常。'}</p></div>
          {showDetails ? <button className="pc-secondary" type="button" disabled={disabled} onClick={() => setShowDetails(false)}><ProfileIcon name="back" />返回个人中心</button> : <span className="pc-heading-doodle" aria-hidden="true">♡<Paw /></span>}
        </div>
        <div className="pc-toolbar">      <button type="button" disabled={disabled} onClick={() => void run(async (token) => {
        applyUser(await getCurrentUser(token), token)
        if (getToken() === token) setAvatarAttempt((value) => value + 1)
        return '资料已刷新'
      })}>刷新资料</button></div>
        <p className="pc-notice" role="status" aria-live="polite">{busy ? '正在处理…' : message || logoutMessage}</p>
        {showDetails ? (
          <div className="pc-detail-grid">
            <div className="pc-card pc-edit-card">
                    <section className="pc-avatar-editor" aria-labelledby="avatar-title">
        <h2 id="avatar-title">更换头像</h2>
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
                    <section className="pc-form-section" aria-labelledby="profile-title">
        <h2 id="profile-title">编辑基本资料</h2>
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
                    <section className="pc-form-section" aria-labelledby="contact-title">
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
                    <details className="pc-password"><summary id="password-title">修改登录密码<span>展开设置</span></summary>
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
      </details>
              <button className="pc-cancel pc-secondary" type="button" disabled={disabled} onClick={() => setShowDetails(false)}>返回个人中心</button>
            </div>
            <aside className="pc-detail-aside">
              <section className="pc-card pc-account" aria-labelledby="account-title">
                <h2 id="account-title">账号信息</h2>
                <dl><div><dt>账号</dt><dd>{user.username}</dd></div>
                  <div><dt>注册时间</dt><dd>{formatTime(user.createdAt)}</dd></div>
                  <div><dt>最近登录</dt><dd>{formatTime(user.lastLoginAt ?? user.LastLoginAt)}</dd></div></dl>
              </section>
              <div className="pc-decoration"><p>认真记录，温柔陪伴<Paw /></p><img src="/images/profile-pets.png" alt="" /></div>
            </aside>
          </div>
        ) : (
          <div className="pc-overview-grid">
            <aside className="pc-identity">
              <div className="pc-avatar-wrap"><Avatar key={`${user.avatarUrl}:${avatarAttempt}`} path={user.avatarUrl} onSessionEnded={onSessionEnded} /><span className="pc-avatar-badge" aria-hidden="true"><Paw /></span></div>
              <h2>{user.nickname || user.username}</h2>
              <p className="pc-account-label">账号</p><p className="pc-username">{user.username}</p>
              <div className="pc-identity-action"><button className="pc-primary" type="button" disabled={disabled} onClick={() => setShowDetails(true)}><ProfileIcon name="edit" />编辑资料</button><p>查看和修改详细资料</p></div>
              <img className="pc-identity-pets" src="/images/profile-pets.png" alt="" />
            </aside>
            <div className="pc-overview-content">
              <section className="pc-card pc-overview-card" aria-labelledby="overview-title">
                <h2 id="overview-title">基本资料</h2>
                <dl className="pc-facts">
                  <div><ProfileIcon name="cake" /><div><dt>年龄</dt><dd>{user.age == null ? '未填写' : `${user.age} 岁`}</dd></div></div>
                  <div><ProfileIcon name="person" /><div><dt>性别</dt><dd>{user.gender === 1 ? '男' : user.gender === 2 ? '女' : '未填写'}</dd></div></div>
                  <div><ProfileIcon name="calendar" /><div><dt>生日</dt><dd>{user.birthday ? user.birthday.replace(/^(\d{4})-(\d{2})-(\d{2})$/, '$1年$2月$3日') : '未填写'}</dd></div></div>
                </dl>
                <div className="pc-bio"><h2>个人简介</h2><div className="pc-bio-text"><span aria-hidden="true">“</span><p>{user.bio || '还没有填写个人简介，点击“编辑资料”介绍一下自己吧。'}</p></div><div className="pc-bio-paws" aria-hidden="true"><Paw /><Paw /></div></div>
              </section>
              <div className="pc-motto"><Paw /><p>把平凡的小日子，过成温柔的陪伴。</p><span aria-hidden="true">♡</span></div>
            </div>
          </div>
        )}
      </main>
      <footer className="pc-footer">PetCure · 记录日常，陪伴每一次成长</footer>
    </div>
  )
}
