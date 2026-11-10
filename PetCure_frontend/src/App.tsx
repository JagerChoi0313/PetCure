//App.tsx是应用的主组件，负责组织和展示页面内容。main.tsx是程序的入口，负责启动React应用
import {useState,type FormEvent} from 'react'
//useState:保存用户输入

export default function App(){

  const [username,setUsername] = useState('')
  const [password,setPassword] = useState('')
  const [message,setMessage] = useState('')
  const [submitting,setSubmitting] = useState(false)
  const [displayName, setDisplayName] = useState<string | null>(null)

 

  async function handleSubmit(event:FormEvent<HTMLFormElement>){    
    event.preventDefault()    //点击登录后‘handleSubmit’里的event.preventDefault()会阻止浏览器按照默认方式提交表单刷新页面
                              //下一步就在这个函数里把username和password组装成JSON，发给POST/api/auth/login
    setMessage('')
    setSubmitting(true)
    

    try{
      const response = await fetch('/api/auth/login',{
        method: 'POST',
        headers:{'Content-Type':'application/json'},
        body:JSON.stringify({username,password})
      })

      const result = await response.json().catch(()=>null)//如果前面发生错误就返回null

      if(!response.ok || result?.code!== 'SUCCESS'){
        setMessage(result?.message || `登录失败(HTTP${response.status})`)
        return
      }

      const token=result.data?.accessToken
      if(typeof token!=='string'||!token){
        setMessage('登录响应缺少访问令牌')
        return
      }

      sessionStorage.setItem('petcure.accessToken',token)
      setPassword('')
      const meResponse = await fetch(`/api/users/me`,{
        headers:{Authorization:`Bearer ${token}`},

      })

      if (meResponse.status == 401){
        sessionStorage.removeItem(`petcure.accessToken`)
        setMessage('登录状态已失效，请重新登录')
        return
      }

      const me = await meResponse.json().catch(() => null)

      if(!meResponse.ok || me?.code != 'SUCCESS'||!me.data){
        setMessage('登录成功，但我读取用户资料失败')
        return
      }

      setDisplayName(me.data.nickname || me.data.username)
    }catch{
      setMessage('登录请求失败，请检查前端和后端是否正在运行')
    }finally{
      setSubmitting(false)
    }
  }                        
  



   async function handleLogout(){
   const token = sessionStorage.getItem('petcure.accessToken')
   setMessage('')
   setSubmitting(true)

   try{
    if(token){
      const response = await fetch('/api/auth/logout',{
        method:'POST',
        headers:{Authorization:`Bearer ${token}`}
      })

      if(!response.ok && response.status!==401){
        setMessage(`退出失败(HTTP${response.status})，请重试`)
        return
      }
    }

    sessionStorage.removeItem(`petcure.accessToken`)
    setDisplayName(null)
    setMessage('已退出登录')
   }catch{
     setMessage('暂时无法连接后端，请重试退出')
   }finally{
     setSubmitting(false)
   }
  }

  if (displayName !== null) {
  return (
    <main>
      <h1>PetCure</h1>
      <p>欢迎，{displayName}</p>
      <button type="button" onClick={handleLogout} disabled={submitting}>
        {submitting ? '退出中…' : '退出登录'}
      </button>
      <p role="status">{message}</p>
    </main>
  )
}
  return(
    <main>
      <h1>PetCure 登录</h1>

      <form onSubmit={handleSubmit}>
        <p>
          <label htmlFor="username">用户名</label>
          <br />
          <input
            id="username"
            autoComplete="username"
            value={username}
            onChange={(event) => setUsername(event.target.value)}   //onChange:在每次输入时更新对应的值
            required
          />
        </p>

        <p>
          <label htmlFor="password">密码</label>
          <br />
          <input
            id="password"
            type="password"
            autoComplete="current-password"
            value={password}
            onChange={(event) => setPassword(event.target.value)}
            required
          />
        </p>

         <button type="submit" disabled={submitting}>
          {submitting ? '登录中…' : '登录'}
          </button>
        <p role="status">{message}</p>
      </form>
    </main>
  )
}