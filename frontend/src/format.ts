/** Small helpers. The server speaks UTC (ISO strings); people see their own local time. */

export function formatDateTime(iso?: string): string {
  if (!iso) return '—'
  return new Date(iso).toLocaleString(undefined, { dateStyle: 'medium', timeStyle: 'short' })
}

export function errorText(err: unknown): string {
  return err instanceof Error ? err.message : 'Something went wrong.'
}

/** "5" -> 5, "" or junk -> undefined (so the form can say "enter a number"). */
export function wholeNumber(text: string): number | undefined {
  const n = Number(text)
  return text.trim() !== '' && Number.isInteger(n) && n > 0 ? n : undefined
}

/** A link that opens WhatsApp chat with a number, optionally with the message already typed. */
export function whatsappLink(phone: string, message?: string): string {
  const country = (import.meta.env.VITE_DEFAULT_COUNTRY_CODE as string | undefined) ?? '91'
  let digits = phone.replace(/\D/g, '')
  if (!phone.trim().startsWith('+') && digits.length === 10) digits = country + digits
  return `https://wa.me/${digits}` + (message ? `?text=${encodeURIComponent(message)}` : '')
}
