import { defineStore } from 'pinia'

// 登录态只保存在 sessionStorage：刷新页面不丢失，但关闭浏览器即清空，
// 下次再打开登录页不会自动登录。启动时清理旧版本遗留在 localStorage 的凭证。
localStorage.removeItem('token')
localStorage.removeItem('user')

export const useUserStore = defineStore('user', {
  state: () => ({
    token: sessionStorage.getItem('token') || '',
    user: JSON.parse(sessionStorage.getItem('user') || 'null'),
  }),
  getters: {
    isLogin: (state) => !!state.token,
    isAdmin: (state) => state.user?.role === 'admin',
    isStudent: (state) => state.user?.role === 'student',
    realName: (state) => state.user?.realName || state.user?.username || '',
  },
  actions: {
    setLogin(token, user) {
      this.token = token
      this.user = user
      sessionStorage.setItem('token', token)
      sessionStorage.setItem('user', JSON.stringify(user))
    },
    logout() {
      this.token = ''
      this.user = null
      sessionStorage.removeItem('token')
      sessionStorage.removeItem('user')
    },
  },
})