import type { ReactNode } from 'react'
import './auth.css'

export default function AuthLayout({ children }: { children: ReactNode }) {
  return (
    <main className="auth-page">
      <div className="auth-shell">
        <section className="auth-art" aria-label="PetCure 宠物日常管理">
          <div className="auth-brand"><svg viewBox="0 0 64 64" aria-hidden="true"><ellipse cx="14" cy="19" rx="7" ry="10" transform="rotate(-28 14 19)"/><ellipse cx="28" cy="11" rx="7" ry="10"/><ellipse cx="43" cy="13" rx="7" ry="10" transform="rotate(18 43 13)"/><ellipse cx="54" cy="27" rx="7" ry="10" transform="rotate(35 54 27)"/><path d="M14 44c0-8 10-20 18-20s20 14 20 23c0 16-13 7-20 7s-18 7-18-10Z"/></svg>PetCure</div>
          <p className="auth-tagline">记录日常，陪伴每一次成长</p>
          <img className="auth-pets" src="/images/petcure-pets.png" alt="依偎在一起的猫咪和柯基，身边摆着绿植和小球" />
        </section>
        <section className="auth-card">{children}</section>
      </div>
    </main>
  )
}
