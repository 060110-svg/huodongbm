import { ref } from 'vue'

export const confirmRef = ref(null)

export async function confirm(options) {
  if (!confirmRef.value) return false
  return confirmRef.value.open(options)
}