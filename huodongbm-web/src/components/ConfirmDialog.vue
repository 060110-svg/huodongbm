<script setup>
import { ref, computed } from 'vue'

const visible = ref(false)
let resolver = null

const state = ref({ title: '', message: '', okText: '确定', okClass: 'danger' })

const iconMeta = computed(() => {
  switch (state.value.okClass) {
    case 'warning': return { icon: '⚠️', cls: 'warning' }
    case 'primary': return { icon: '✅', cls: 'primary' }
    case 'success': return { icon: '✅', cls: 'success' }
    case 'info': return { icon: 'ℹ️', cls: 'info' }
    default: return { icon: '🗑️', cls: 'danger' }
  }
})

function open({ title = '确认操作', message = '确定执行该操作吗？', okText = '确定', okClass = 'danger' } = {}) {
  state.value = { title, message, okText, okClass }
  visible.value = true
  return new Promise((resolve) => { resolver = resolve })
}

function onOk() { visible.value = false; resolver?.(true); resolver = null }
function onCancel() { visible.value = false; resolver?.(false); resolver = null }

defineExpose({ open })
</script>

<template>
  <div class="confirm-overlay" :class="{ show: visible }" @click.self="onCancel">
    <div class="confirm-dialog">
      <div class="confirm-icon" :class="iconMeta.cls">{{ iconMeta.icon }}</div>
      <div class="confirm-title">{{ state.title }}</div>
      <div class="confirm-message">{{ state.message }}</div>
      <div class="confirm-btns">
        <button class="confirm-cancel" @click="onCancel">取消</button>
        <button class="confirm-ok" :class="state.okClass" @click="onOk">{{ state.okText }}</button>
      </div>
    </div>
  </div>
</template>