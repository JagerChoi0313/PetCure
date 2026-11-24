export function Paw() {
  return <svg viewBox="0 0 64 64" aria-hidden="true" fill="currentColor"><ellipse cx="13" cy="23" rx="7" ry="10" transform="rotate(-25 13 23)" /><ellipse cx="27" cy="13" rx="7" ry="10" /><ellipse cx="43" cy="15" rx="7" ry="10" transform="rotate(18 43 15)" /><ellipse cx="54" cy="29" rx="7" ry="10" transform="rotate(30 54 29)" /><path d="M14 46c0-8 10-20 18-20s20 14 20 23c0 15-13 6-20 6s-18 6-18-9Z" /></svg>
}

const paths = {
  edit: 'm15 5 4 4M4 20l4-1L20 7a2.8 2.8 0 0 0-4-4L4 15Z',
  logout: 'M9 4H4v16h5M9 12h12m-5-5 5 5-5 5',
  back: 'M20 12H4m6-6-6 6 6 6',
  cake: 'M4 10h16v10H4ZM8 10V7m8 3V7M4 14c3 4 5-3 8 0s5 1 8 0M8 3v1m8-1v1',
  person: 'M16 7a4 4 0 1 1-8 0 4 4 0 0 1 8 0ZM4 21v-2a8 8 0 0 1 16 0v2',
  calendar: 'M4 5h16v16H4ZM8 3v4m8-4v4M4 10h16M8 14h1m6 0h1m-8 3h1',
}
export function ProfileIcon({ name }: { name: keyof typeof paths }) {
  return <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.6" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true"><path d={paths[name]} /></svg>
}
