/** Small hand-drawn style icons used on the home page cards. Decorative, so hidden from screen readers. */
const common = { width: 56, height: 56, viewBox: '0 0 56 56', 'aria-hidden': true, focusable: false } as const

export function IconNeed() {
  return (
    <svg {...common}>
      <circle cx="28" cy="28" r="26" fill="#FDE9D4" />
      <path d="M28 41C14 32 14 20 21 18c4-1 6 1 7 4 1-3 3-5 7-4 7 2 7 14-7 23z" fill="#C23B22" />
      <path d="M18 44c4-3 8-3 10-1 2-2 6-2 10 1" stroke="#14452F" strokeWidth="3" strokeLinecap="round" fill="none" />
    </svg>
  )
}

export function IconSurplus() {
  return (
    <svg {...common}>
      <circle cx="28" cy="28" r="26" fill="#FBEFC2" />
      <path d="M12 30h32c0 9-7 14-16 14s-16-5-16-14z" fill="#14452F" />
      <path d="M18 28c0-7 5-10 10-10s10 3 10 10z" fill="#F4B41A" />
      <path d="M22 14c-2-3 2-4 0-7M28 13c-2-3 2-4 0-7M34 14c-2-3 2-4 0-7" stroke="#14452F" strokeWidth="2.4" strokeLinecap="round" fill="none" />
    </svg>
  )
}

export function IconDeliver() {
  return (
    <svg {...common}>
      <circle cx="28" cy="28" r="26" fill="#D8EFDF" />
      <circle cx="16" cy="40" r="6" fill="none" stroke="#14452F" strokeWidth="3" />
      <circle cx="42" cy="40" r="6" fill="none" stroke="#14452F" strokeWidth="3" />
      <path d="M16 40h-3l3-12h10l4 12h9M30 40l-3-12" stroke="#14452F" strokeWidth="3" strokeLinecap="round" strokeLinejoin="round" fill="none" />
      <rect x="26" y="14" width="16" height="12" rx="2" fill="#F4B41A" />
    </svg>
  )
}
