<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getHome } from '../api/auth'
import StatusTag from '../components/StatusTag.vue'
import { fmtDate } from '../utils/format'

const router = useRouter()
const data = ref(null)
const loading = ref(true)
const errorMsg = ref('')

onMounted(async () => {
  try {
    const res = await getHome()
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
      <div class="home-hero">
        <div class="home-hero-content">
          <h2>👋 你好，{{ data.user.realName }}！</h2>
          <p>欢迎回到校园活动报名系统，发现精彩活动，开启美好校园生活！</p>
        </div>
        <div class="home-hero-emoji">🎉</div>
      </div>

      <div class="stat-grid">
        <div class="stat-card gradient-1">
          <div class="stat-icon">📚</div>
          <div class="stat-number">{{ data.totalActivities }}</div>
          <div class="stat-label">全部活动</div>
        </div>
        <div class="stat-card gradient-2">
          <div class="stat-icon">🚀</div>
          <div class="stat-number">{{ data.ongoingActivities }}</div>
          <div class="stat-label">进行中</div>
        </div>
        <div class="stat-card gradient-3">
          <div class="stat-icon">🎫</div>
          <div class="stat-number">{{ data.myTotal }}</div>
          <div class="stat-label">我的报名</div>
        </div>
        <div class="stat-card gradient-4">
          <div class="stat-icon">✅</div>
          <div class="stat-number">{{ data.myApproved }}</div>
          <div class="stat-label">已通过</div>
        </div>
      </div>

      <div class="quick-nav">
        <a @click="router.push('/activities')">
          <div class="qn-emoji">📋</div>
          <div class="qn-text">浏览活动</div>
        </a>
        <a @click="router.push('/my-registrations')">
          <div class="qn-emoji">🎫</div>
          <div class="qn-text">我的报名</div>
        </a>
        <a @click="router.push('/activities')">
          <div class="qn-emoji">🔥</div>
          <div class="qn-text">热门活动</div>
        </a>
      </div>

      <div class="section-card">
        <div class="section-title">
          🔥 热门活动 TOP5
          <router-link to="/activities">查看全部 →</router-link>
        </div>
        <div v-if="data.hotActivities && data.hotActivities.length">
          <div v-for="a in data.hotActivities" :key="a.id" class="activity-row">
            <div class="act-emoji">🔥</div>
            <div class="act-info" @click="router.push(`/activities/${a.id}`)">
              <div class="act-title">{{ a.title }}</div>
              <div class="act-meta">{{ a.location || '待定' }} · 已报 {{ a.registeredCount ?? 0 }} 人</div>
            </div>
            <button class="act-join" @click="router.push(`/activities/${a.id}`)">去看看</button>
          </div>
        </div>
        <div v-else class="empty-hint">暂无热门活动</div>
      </div>

      <div class="section-card">
        <div class="section-title">
          📋 我的最近报名
          <router-link to="/my-registrations">查看全部 →</router-link>
        </div>
        <div v-if="data.myLatest && data.myLatest.length">
          <div v-for="r in data.myLatest" :key="r.id" class="my-row">
            <div class="my-dot" :class="r.status === 'approved' ? 'ok' : r.status === 'registered' ? 'reg' : r.status === 'rejected' ? 'rej' : 'cancel'"></div>
            <div class="my-info" @click="router.push(`/activities/${r.activityId}`)">
              <div class="my-title">{{ r.activityTitle || '（活动已删除）' }}</div>
              <div class="my-time">{{ r.status === 'approved' ? '已通过' : r.status === 'registered' ? '待审核' : r.status === 'rejected' ? '已拒绝' : '已取消' }} · {{ fmtDate(r.registerTime) }}</div>
            </div>
          </div>
        </div>
        <div v-else class="empty-hint">还没有报名任何活动</div>
      </div>
    </template>
  </div>
</template>