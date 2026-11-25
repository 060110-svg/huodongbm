<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '../stores/user'
import { login } from '../api/auth'

const router = useRouter()
const userStore = useUserStore()

const username = ref('')
const password = ref('')
const loading = ref(false)
const errorMsg = ref('')

async function handleLogin() {
  errorMsg.value = ''
  if (!username.value.trim() || !password.value) {
    errorMsg.value = '请输入用户名和密码'
    return
  }
  loading.value = true
  try {
    const res = await login(username.value.trim(), password.value)
    userStore.setLogin(res.data.token, res.data.user)
    router.push(userStore.isAdmin ? '/dashboard' : '/home')
  } catch (e) {
    errorMsg.value = e.message
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="login-page">
    <div class="bg-grid"></div>
    <div class="floating-icons">
      <span class="float-icon">🎓</span>
      <span class="float-icon">📚</span>
      <span class="float-icon">🎯</span>
      <span class="float-icon">✨</span>
      <span class="float-icon">🎉</span>
    </div>

    <div class="login-box">
      <div class="logo">🎓</div>
      <h2>校园活动报名系统</h2>
      <p class="subtitle">加入精彩校园生活，<br />遇见有趣的灵魂 ✨</p>

      <div v-if="errorMsg" class="error-msg">⚠️ {{ errorMsg }}</div>

      <form @submit.prevent="handleLogin">
        <div class="form-item">
          <label>👤 用户名</label>
          <input v-model="username" type="text" placeholder="请输入用户名" autocomplete="username" />
        </div>
        <div class="form-item">
          <label>🔒 密码</label>
          <input v-model="password" type="password" placeholder="请输入密码" autocomplete="current-password" />
        </div>
        <button type="submit" class="login-btn" :disabled="loading">{{ loading ? '登 录 中...' : '开 始 校 园 之 旅' }}</button>
      </form>

      <div class="tips">
        🍀 测试账号
        <div class="user-types">
          <span class="user-type admin-type">👨‍💼 管理员：<code>admin</code></span>
          <span class="user-type student-type">🎓 学生：<code>student01</code></span>
        </div>
        <div style="margin-top:8px;">密码统一：<code>123456</code></div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.login-page {
  min-height: 100vh;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 50%, #f093fb 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
  overflow: hidden;
}
.bg-grid {
  position: fixed; top: -50%; left: -50%;
  width: 200%; height: 200%;
  background-image:
    radial-gradient(circle at 25% 25%, rgba(255,255,255,0.15) 1px, transparent 1px),
    radial-gradient(circle at 75% 75%, rgba(255,255,255,0.1) 1px, transparent 1px);
  background-size: 40px 40px, 60px 60px;
  animation: drift 25s linear infinite;
}
@keyframes drift {
  0% { transform: translate(0, 0) rotate(0deg); }
  100% { transform: translate(40px, 40px) rotate(360deg); }
}
.floating-icons { position: absolute; inset: 0; pointer-events: none; z-index: 0; }
.float-icon { position: absolute; opacity: 0.25; animation: float-up 8s ease-in-out infinite; font-size: 40px; }
.float-icon:nth-child(1) { top: 10%; left: 10%; animation-delay: 0s; font-size: 50px; }
.float-icon:nth-child(2) { top: 20%; right: 15%; animation-delay: 1.5s; font-size: 45px; }
.float-icon:nth-child(3) { bottom: 25%; left: 20%; animation-delay: 3s; font-size: 55px; }
.float-icon:nth-child(4) { bottom: 10%; right: 10%; animation-delay: 4.5s; font-size: 40px; }
.float-icon:nth-child(5) { top: 50%; left: 5%; animation-delay: 2s; font-size: 35px; }
@keyframes float-up {
  0%, 100% { transform: translateY(0) rotate(0deg); }
  50% { transform: translateY(-20px) rotate(10deg); }
}
.login-box {
  width: 440px;
  background: rgba(255,255,255,0.97);
  padding: 50px 42px;
  border-radius: 32px;
  box-shadow: 0 30px 90px rgba(0,0,0,0.3), 0 0 0 1px rgba(255,255,255,0.2);
  position: relative;
  z-index: 1;
}
.logo {
  width: 80px; height: 80px;
  margin: 0 auto 22px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 24px;
  display: flex; align-items: center; justify-content: center;
  font-size: 44px;
  box-shadow: 0 12px 35px rgba(102,126,234,0.45);
  animation: pulse 3s ease-in-out infinite;
}
@keyframes pulse {
  0%, 100% { transform: scale(1); }
  50% { transform: scale(1.05); }
}
.login-box h2 { text-align: center; color: #1d1d1f; font-weight: 800; font-size: 28px; margin-bottom: 6px; letter-spacing: 1px; }
.subtitle { text-align: center; color: #86868b; font-size: 14px; margin-bottom: 38px; line-height: 1.6; }
.form-item { margin-bottom: 24px; }
.form-item label { display: block; color: #333336; font-size: 13px; font-weight: 600; margin-bottom: 10px; }
.form-item input {
  width: 100%;
  padding: 15px 18px;
  border: 2px solid #e5e5ea;
  border-radius: 14px;
  font-size: 15px;
  background: #fafafa;
  outline: none;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  box-sizing: border-box;
  font-family: inherit;
}
.form-item input:focus {
  border-color: #667eea;
  background: #fff;
  box-shadow: 0 0 0 5px rgba(102,126,234,0.15);
  transform: translateY(-1px);
}
.login-btn {
  width: 100%;
  padding: 17px;
  border: none;
  border-radius: 16px;
  font-size: 16px;
  font-weight: 700;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: #fff;
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  margin-top: 10px;
  box-shadow: 0 10px 30px rgba(102,126,234,0.4);
  letter-spacing: 4px;
  font-family: inherit;
}
.login-btn:hover:not(:disabled) { transform: translateY(-3px); box-shadow: 0 15px 40px rgba(102,126,234,0.5); }
.login-btn:active { transform: translateY(0); }
.login-btn:disabled { opacity: 0.7; cursor: not-allowed; }
.error-msg {
  color: #ff3b30;
  font-size: 14px;
  text-align: center;
  margin-bottom: 20px;
  padding: 12px 16px;
  background: linear-gradient(135deg, #fff5f5, #ffe8e8);
  border-radius: 12px;
  border: 1px solid #ffd1d1;
}
.tips {
  margin-top: 28px;
  padding-top: 22px;
  border-top: 2px dashed #e5e5ea;
  font-size: 12px;
  color: #86868b;
  text-align: center;
  line-height: 2;
}
.tips code {
  background: linear-gradient(135deg, #eef2ff, #f3e8ff);
  padding: 3px 10px;
  border-radius: 6px;
  color: #667eea;
  font-family: inherit;
  font-weight: 600;
  margin: 0 2px;
}
.user-types { margin-top: 10px; display: flex; justify-content: center; gap: 10px; }
.user-type { padding: 6px 14px; border-radius: 20px; font-size: 11px; font-weight: 600; }
.admin-type { background: #fef3c7; color: #92400e; }
.student-type { background: #dbeafe; color: #1e40af; }
</style>