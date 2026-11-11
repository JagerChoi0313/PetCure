// import {useState,type FormEvent} from 'react'//导入react和typeScript类型


// //定义组件的Props类型
// //这是typeScript最基础，最重要的语法之一
// //这里的type用来定义一个类型，可以理解为我们创建了一个名字叫RegisterFormProps的类型
// //它规定了这个组件需要接收什么参数
// type RegisterFormProps = {
    
//     //这里的意思是onRegistered是一个函数，这个函数需要接收一个参数
//     //参数的名称叫做username，参数的类型是string，也就是字符串
//     //=>void：表示函数不需要返回有意义的值
//     onRegistered: (username: string)=>void
//     onBack:() =>void
// }

// //记住这个语法即可：
// //type 类型名称 ={
// //      属性名称：属性类型
// // }
// //它可以定义字符串、数字、布尔值、函数等各种类型

// //定义react组件和状态
// export default function RegisterForm({
//      //函数参数的解构
//      //正常情况下我们这样写
// //      function RegisterForm(props: RegisterFormProps) {
// //     props.onRegistered('Tom')
// //     props.onBack()
// // }
// //但我们现在这种写法用的是对象解构：
//     onRegistered,      
//     onBack,
// //这样就可以直接使用onRegistered和onBack,不需要每次写props


// }:RegisterFormProps){//参数类型标注
//     //表示这个函数接收的对象必须符合 RegisterFormProps 定义的类型。
//     //所以上面的代码可以理解为创建一个RegisterForm函数，它接收一个对象，这个对象必须符合RegisterFormProps规定的属性
//     const [message,setMessage] = useState('')
//     const [submitting,setSubmitting] = useState(false)

//     //编写注册处理函数
//     async function handleRegister(event:
//         FormEvent<HTMLFormElement>) //FormEvent：泛型类型
//         {
//             event.preventDefault()
//             setMessage('')
//             //验证信息
//             //"??"是指空值合并运算符。如果左边是null或undefined，就使用右边的值
//             const form = new FormData(event.currentTarget)
//             const username = String(form.get('username')??'').trim()
//             const password = String(form.get('password') ?? '')
//             const confirmPassword = String(form.get('confirmPassword')?? '')
//             const phone = String(form.get('phone')??'')
//             const email = String(form.get('email') ?? '').trim()
//             if (password !== confirmPassword) {
//       setMessage('两次输入的密码不一致')
//       return
//     }

//     if (!phone && !email) {
//       setMessage('手机号和邮箱至少填写一项')
//       return
//     }

//     setSubmitting(true)

//     //发送请求
//     try {
//       const response = await fetch('/api/auth/register', {
//         method: 'POST',
//         headers: { 'Content-Type': 'application/json' },
//         body: JSON.stringify({
//           username,
//           password,
//           confirmPassword,
//           phone: phone || null,
//           email: email || null,
//         }),
//       })

//       //处理结果
//       const result = await response.json().catch(() => null)

//       if (!response.ok || result?.code !== 'SUCCESS') {
//         setMessage(result?.message || `注册失败（HTTP ${response.status}）`)
//         return
//       }

//       onRegistered(username)
//     } catch {
//       setMessage('注册请求失败，请检查后端是否正在运行')
//     } finally {
//       setSubmitting(false)
//     }

//         }
    
//     //返回页面的JSX结构
//     return(
//              <main>
//       <h1>PetCure 注册</h1>

//       <form onSubmit={handleRegister}>
//         <p>
//           <label>用户名 <input name="username" autoComplete="username" required /></label>
//         </p>
//         <p>
//           <label>
//             密码 <input name="password" type="password" autoComplete="new-password" minLength={8} required />
//           </label>
//         </p>
//         <p>
//           <label>
//             确认密码 <input name="confirmPassword" type="password" autoComplete="new-password" required />
//           </label>
//         </p>
//         <p>
//           <label>手机号 <input name="phone" type="tel" placeholder="含国家区号，如 +86" /></label>
//         </p>
//         <p>
//           <label>邮箱 <input name="email" type="email" /></label>
//         </p>

//         <button type="submit" disabled={submitting}>
//           {submitting ? '注册中…' : '注册'}
//         </button>
//         <button type="button" onClick={onBack} disabled={submitting}>
//           返回登录
//         </button>
//         <p role="status">{message}</p>
//       </form>
//     </main>
//         )
// }


// //typeScript = JavaScript+类型系统
// //typeScript并不是一门完全独立的编程语言，而是在JavaScript的基础上增加静态类型检查等能力
// //你可以把它理解为：JavaScript负责实现程序的功能
// //typeScript在编写代码时帮助你检查数据类型、函数参数和返回值是否正确

// //最明显的区别：变量没有类型限制
// //假设我们要定义一个用户名
// //JavaScript：
// // let username = 'Tom'
// // username = 123
// // username = true
// //这些代码Javascript都是运行执行的，因为JavaScript是动态类型语言，变量可以在运行过程中保存不同类型的数据


// //TypeScript
// //let username: string = 'Tom'
// // username = 'Jerry'  // 正确
// // username = 123      // 报错
// // username = true     // 报错

// //因为我们明确规定了username只能保存字符串

// //参数类型检查：
// // onRegistered: (username: string) => void
// //这里表示onRegister必须接收一个字符串

// //但在JavaScript中，JavaScript 不会因为参数类型不符合预期而自动报错。

// // function onRegistered(username) {
// //     console.log(username)
// // }
// // onRegistered('Tom')
// // onRegistered(123)
// // onRegistered(true)

// // TypeScript 中：
// // function onRegistered(username: string): void {
// //     console.log(username)
// // }
// // onRegistered('Tom')  // 正确
// // onRegistered(123)    // 类型错误

// //TypeScript 可以定义复杂的数据结构
// //这是TypeScript 特别有价值的地方。

// //JavaScript：
// // const user = {
// //     username: 'Tom',
// //     age: 18,
// //     email: 'tom@example.com',
// //     isAdmin: false
// // }
// //JavaScript 不会强制规定这个对象必须包含哪些字段。

// // TypeScript：
// // type User = {
// //     username: string
// //     age: number
// //     email: string
// //     isAdmin: boolean
// // }

// // const user: User = {
// //     username: 'Tom',
// //     age: 18,
// //     email: 'tom@example.com',
// //     isAdmin: false
// // }

// // 相当于定义了一个用户数据模板。

// // 以后你创建用户对象，都可以按照这个模板进行类型检查。

// //TypeScript 有泛型
// //const [message, setMessage] = useState('')
// //也可以写成
// //const [message, setMessage] = useState<string>('')
// //其中<string>就是泛型参数

// // JavaScript 本身没有 TypeScript 这种编译期泛型类型系统。

// // TypeScript 的泛型允许我们编写可以处理多种数据类型，同时又保留类型检查能力的代码。


// // 例如
// // function getData<T>(data: T): T {
// //     return data
// // }
// // const a = getData<string>('Hello')
// // const b = getData<number>(100)

// // 这里：
// // - T 是一个类型参数。
// // - 第一次调用，T 是 string。
// // - 第二次调用，T 是 number。


import { useState, type FormEvent } from 'react'
import { getErrorMessage, registerUser } from './api'

type Props = {
  onRegistered: (username: string) => void
  onBack: () => void
}

export default function RegisterForm({ onRegistered, onBack }: Props) {
  const [message, setMessage] = useState('')
  const [submitting, setSubmitting] = useState(false)

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setMessage('')

    const form = new FormData(event.currentTarget)
    const username = String(form.get('username') ?? '').trim()
    const password = String(form.get('password') ?? '')
    const confirmPassword = String(form.get('confirmPassword') ?? '')
    const phone = String(form.get('phone') ?? '').trim()
    const email = String(form.get('email') ?? '').trim()

    if (password !== confirmPassword) {
      setMessage('两次输入的密码不一致')
      return
    }

    if (!phone && !email) {
      setMessage('手机号和邮箱至少填写一项')
      return
    }

    if (new TextEncoder().encode(password).length > 72) {
      setMessage('密码 UTF-8 编码后的长度不能超过72字节')
      return
    }

    setSubmitting(true)

    try {
      await registerUser({
        username,
        password,
        confirmPassword,
        phone: phone || null,
        email: email || null,
      })
      onRegistered(username)
    } catch (error) {
      setMessage(getErrorMessage(error))
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <main>
      <h1>PetCure 注册</h1>

      <form onSubmit={handleSubmit}>
        <fieldset disabled={submitting}>
          <legend>注册信息</legend>

          <p>
            <label>
              用户名<br />
              <input
                name="username"
                autoComplete="username"
                minLength={4}
                maxLength={50}
                pattern="[a-zA-Z0-9_]+"
                title="4到50位字母、数字或下划线"
                required
              />
            </label>
          </p>

          <p>
            <label>
              密码<br />
              <input
                name="password"
                type="password"
                autoComplete="new-password"
                minLength={8}
                maxLength={64}
                required
              />
            </label>
          </p>

          <p>
            <label>
              确认密码<br />
              <input
                name="confirmPassword"
                type="password"
                autoComplete="new-password"
                maxLength={64}
                required
              />
            </label>
          </p>

          <p>
            <label>
              手机号<br />
              <input
                name="phone"
                type="tel"
                autoComplete="tel"
                placeholder="含国家区号，如 +86"
                pattern="\+[1-9][0-9]{6,14}"
                title="以加号和国家区号开头，共7到15位数字"
              />
            </label>
          </p>

          <p>
            <label>
              邮箱<br />
              <input
                name="email"
                type="email"
                autoComplete="email"
                maxLength={254}
              />
            </label>
          </p>

          <p>手机号和邮箱至少填写一项。</p>
          <button type="submit">
            {submitting ? '注册中…' : '注册'}
          </button>
          <button type="button" onClick={onBack}>返回登录</button>
        </fieldset>
      </form>

      <p role="status">{message}</p>
    </main>
  )
}