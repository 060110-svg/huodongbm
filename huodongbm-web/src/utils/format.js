export function fmtDate(value) {
  if (!value) return '-'
  return String(value).length >= 16 ? String(value).slice(0, 16) : String(value)
}

// 判断后端返回的时间字符串（"yyyy-MM-dd HH:mm:ss"，服务器本地时区）是否已过去
export function isPast(value) {
  if (!value) return false
  const t = new Date(String(value).replace(' ', 'T')).getTime()
  return !isNaN(t) && t < Date.now()
}
