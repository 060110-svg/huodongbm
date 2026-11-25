<script setup>
import { useUserStore } from './stores/user'
import { useRouter } from 'vue-router'
import AppToast from './components/AppToast.vue'
import ConfirmDialog from './components/ConfirmDialog.vue'
import { confirmRef } from './utils/confirm'

const userStore = useUserStore()
const router = useRouter()

function handleLogout() {
  userStore.logout()
  router.push('/login')
}
</script>

<template>
  <div class="layout">
    <header v-if="userStore.isLogin" class="header">
      <h1>🎓 <span class="grad-text">校园活动报名系统</span></h1>
      <nav class="nav">
        <router-link v-if="!userStore.isAdmin" to="/home" active-class="active">首页</router-link>
        <router-link v-if="!userStore.isAdmin" to="/activities" active-class="active">活动广场</router-link>
        <router-link v-if="!userStore.isAdmin" to="/my-registrations" active-class="active">我的报名</router-link>
        <template v-if="userStore.isAdmin">
          <router-link to="/dashboard" active-class="active">概览</router-link>
          <router-link to="/admin/activities" active-class="active">活动管理</router-link>
          <router-link to="/admin/registrations" active-class="active">报名管理</router-link>
        </template>
      </nav>
      <div class="user-info">
        <span class="user-chip" :class="userStore.isAdmin ? 'admin' : 'student'">
          {{ userStore.isAdmin ? '👨‍💼' : '✨' }} {{ userStore.realName }}
        </span>
        <button class="btn btn-danger" @click="handleLogout">退出</button>
      </div>
    </header>
    <main class="content">
      <router-view />
    </main>
    <AppToast />
    <ConfirmDialog ref="confirmRef" />
  </div>
</template>

<style>
.layout { min-height: 100vh; }
.content { min-height: calc(100vh - 70px); }
</style>