import react from '@vitejs/plugin-react'
import { defineConfig } from 'vite'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],     //注册react插件

  //配置Vite转发请求
  server:{
    proxy:{
      '/api':'http://localhost:8080',//浏览器会向前端的5173端口请求/api/auth/login,Vite再把它转发给8080端口的SpringBoot，路径保持/api/auth/login
    }                                 //这是本地开发时用的转发配置
  }
})


//vite是一个前端开发和打包工具，帮助运行react项目，实时查看修改结果，以及打包项目
//vite.config.ts：是 Vite 的配置文件，用来告诉 Vite 项目应该如何运行，比如使用哪些插件、前端端口是多少、如何连接后端等。