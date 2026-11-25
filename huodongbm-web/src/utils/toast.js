import { reactive } from 'vue'

const state = reactive({ toasts: [] })
let seq = 0

function push(type, message) {
  const id = ++seq
  state.toasts.push({ id, type, message })
  setTimeout(() => {
    const i = state.toasts.findIndex((t) => t.id === id)
    if (i > -1) state.toasts.splice(i, 1)
  }, 3000)
}

export const toastStore = state
export function useToast() {
  return {
    success: (m) => push('success', m),
    error: (m) => push('error', m),
    info: (m) => push('info', m),
  }
}