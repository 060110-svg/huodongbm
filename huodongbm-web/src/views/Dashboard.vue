<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getDashboard } from '../api/auth'
import StatusTag from '../components/StatusTag.vue'
import { fmtDate } from '../utils/format'

const router = useRouter()
const data = ref(null)
const loading = ref(true)
const errorMsg = ref('')

onMounted(async () => {
  try {
    const res = await getDashboard()
    data.value = res.data
  } catch (e) {
    errorMsg.value = e.message
  } finally {
    loading.value = false
  }
})
</script>

<template>
  <div class="container">
    <div v-if="loading" class="view-loading">加载中...</div>
    <div v-else-if="errorMsg" class="alert alert-error">{{ errorMsg }}</div>
    <template v-else>
      <div class="welcome">
        <div class="welcome-content">
          <h2>👨‍💼 你好，{{ data.user.realName }}！</h2>
          <p>欢迎回来，在这里你可以高效管理所有校园活动与报名信息。</p>
        </div>
        <div class="welcome-emoji">🎯</div>
      </div>

      <div class="stats">
        <div class="stat-card">
          <div class="icon-circle">📅</div>
          <div class="number">{{ data.activityCount }}</div>
          <div class="label">活动总数</div>
        </div>
        <div class="stat-card">
          <div class="icon-circle">🚀</div>
          <div class="number">{{ data.publishedCount }}</div>
          <div class="label">已发布活动</div>
        </div>
        <div class="stat-card">
          <div class="icon-circle">🎫</div>
          <div class="number">{{ data.registrationCount }}</div>
          <div class="label">待审核报名</div>
        </div>
        <div class="stat-card">
          <div class="icon-circle">👥</div>
          <div class="number">{{ data.userCount }}</div>
          <div class="label">注册用户数</div>
        </div>
      </div>

      <div class="section">
        <div class="section-header">
          <h3>📋 最近发布的活动</h3>
          <router-link to="/admin/activities">管理全部 →</router-link>
        </div>
        <table v-if="data.recentActivities && data.recentActivities.length">
          <thead>
            <tr>
              <th>ID</th>
              <th>活动标题</th>
              <th>地点</th>
              <th>开始时间</th>
              <th>状态</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="a in data.recentActivities" :key="a.id">
              <td>#{{ a.id }}</td>
              <td style="font-weight: 600;">{{ a.title }}</td>
              <td>{{ a.location || '待定' }}</td>
              <td>{{ fmtDate(a.startTime) }}</td>
              <td><StatusTag :status="a.status" /></td>
              <td>
                <a class="tbl-link" @click="router.push(`/activities/${a.id}`)">详情</a>
                <a class="tbl-link" @click="router.push(`/admin/activities/${a.id}/edit`)">编辑</a>
              </td>
            </tr>
          </tbody>
        </table>
        <div v-else class="empty-hint">暂无活动</div>
      </div>
    </template>
  </div>
</template>