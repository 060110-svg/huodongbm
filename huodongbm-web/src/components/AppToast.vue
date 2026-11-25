<script setup>
import { toastStore } from '../utils/toast'
</script>

<template>
  <div class="toast-wrap">
    <transition-group name="toast">
      <div v-for="t in toastStore.toasts" :key="t.id" class="toast" :class="t.type">
        {{ t.message }}
      </div>
    </transition-group>
  </div>
</template>

<style scoped>
.toast-wrap {
  position: fixed; top: 22px; left: 50%; transform: translateX(-50%);
  z-index: 99999; display: flex; flex-direction: column; gap: 10px; align-items: center;
}
.toast {
  padding: 11px 26px; border-radius: 14px; color: #fff; font-size: 14px; font-weight: 600;
  box-shadow: 0 8px 24px rgba(0,0,0,0.18); min-width: 200px; text-align: center;
}
.toast.success { background: linear-gradient(135deg, #11998e, #38ef7d); }
.toast.error { background: linear-gradient(135deg, #ff6b6b, #f5576c); }
.toast.info { background: linear-gradient(135deg, #667eea, #764ba2); }
.toast-enter-active, .toast-leave-active { transition: all .3s ease; }
.toast-enter-from, .toast-leave-to { opacity: 0; transform: translateY(-12px); }
</style>