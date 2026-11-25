<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getActivityDetail } from '../api/activity'
import { joinRegistration, cancelRegistration, getMyRegistrations } from '../api/registration'
import { useUserStore } from '../stores/user'
import StatusTag from '../components/StatusTag.vue'
import { fmtDate, isPast } from '../utils/format'
import { useToast } from '../utils/toast'
import { confirm } from '../utils/confirm'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const toast = useToast()

const activity = ref(null)
const myReg = ref(null)
const loading = ref(true)
const errorMsg = ref('')
const submitting = ref(false)

const id = computed(() => Number(route.params.id))

// 与后端 /api/registrations/join 的校验口径保持一致：
// 仅 published 状态且未过报名截止时间的活动可以报名
const deadlinePassed = computed(() => isPast(activity.value?.deadline))
const isJoinable = computed(() => {
  if (!activity.value) return false
  return activity.value.status === 'published' && !deadlinePassed.value
})
const unjoinableReason = computed(() => {
  if (!activity.value) return ''
  switch (activity.value.status) {
    case 'offline': return '该活动已下线，无法报名'
    case 'ongoing': return '活动正在进行中，无法报名'
    case 'ended': return '活动已结束，无法报名'
    default: return deadlinePassed.value ? '报名截止时间已过，无法报名' : '该活动当前不可报名'
  }
})

onMounted(async () => {
  try {
    const [actRes, myRes] = await Promise.all([
      getActivityDetail(id.value),
      userStore.isStudent ? getMyRegistrations() : Promise.resolve({ data: [] }),
    ])
    activity.value = actRes.data
    if (myRes.data) {
      myReg.value = myRes.data.find(r => r.activityId === id.value && ['registered', 'approved'].includes(r.status)) || null
    }
  } catch (e) {
    errorMsg.value = e.message
  } finally {
    loading.value = false
  }
})

async function handleJoin() {
  submitting.value = true
  try {
    await joinRegistration(id.value)
    toast.success('报名成功，请等待管理员审核')
    const myRes = await getMyRegistrations()
    myReg.value = myRes.data.find(r => r.activityId === id.value && ['registered', 'approved'].includes(r.status)) || null
    const actRes = await getActivityDetail(id.value)
    activity.value = actRes.data
  } catch (e) {
    toast.error(e.message)
  } finally {
    submitting.value = false
  }
}

async function handleCancel() {
  if (!myReg.value) return
  const ok = await confirm({ title: '取消报名', message: `确定取消对「${activity.value.title}」的报名吗？`, okText: '取消报名' })
  if (!ok) return
  submitting.value = true
  try {
    await cancelRegistration(myReg.value.id)
    toast.success('已取消报名')
    myReg.value = null
    const actRes = await getActivityDetail(id.value)
    activity.value = actRes.data
  } catch (e) {
    toast.error(e.message)
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <div class="container-narrow">
    <div v-if="loading" class="view-loading">加载中...</div>
    <div v-else-if="errorMsg" class="not-found">
      <div class="big-icon">😕</div>
      <h2>{{ errorMsg }}</h2>
      <button class="btn btn-primary" @click="router.push('/activities')">返回活动列表</button>
    </div>
    <template v-else-if="activity">
      <button class="back-link" @click="router.back()">← 返回</button>

      <div class="detail-card">
        <img v-if="activity.coverImage" :src="activity.coverImage" class="cover-img" />
        <div v-else class="cover" :class="'c' + ((activity.id % 6) + 1)">{{ activity.title ? activity.title.charAt(0) : '' }}</div>

        <div class="detail-body">
          <div class="title-row">
            <h1 class="detail-title">{{ activity.title }}</h1>
            <span class="status-badge" :class="'badge-' + activity.status">
              {{ activity.status === 'published' ? '🔥 已发布 · 可报名' : activity.status === 'ongoing' ? '🚀 进行中' : activity.status === 'ended' ? '✅ 已结束' : '🚫 已下线' }}
            </span>
          </div>

          <div class="meta-grid">
            <div class="meta-item">
              <div class="meta-icon">📍</div>
              <div>
                <div class="meta-label">活动地点</div>
                <div class="meta-value">{{ activity.location || '待定' }}</div>
              </div>
            </div>
            <div class="meta-item">
              <div class="meta-icon">👥</div>
              <div>
                <div class="meta-label">报名限制</div>
                <div class="meta-value">{{ activity.maxParticipants > 0 ? activity.maxParticipants + ' 人' : '不限人数' }}</div>
              </div>
            </div>
            <div class="meta-item">
              <div class="meta-icon">🕐</div>
              <div>
                <div class="meta-label">开始时间</div>
                <div class="meta-value">{{ fmtDate(activity.startTime) }}</div>
              </div>
            </div>
            <div class="meta-item">
              <div class="meta-icon">🏁</div>
              <div>
                <div class="meta-label">结束时间</div>
                <div class="meta-value">{{ fmtDate(activity.endTime) }}</div>
              </div>
            </div>
            <div v-if="activity.deadline" class="meta-item">
              <div class="meta-icon">⏰</div>
              <div>
                <div class="meta-label">报名截止</div>
                <div class="meta-value">{{ fmtDate(activity.deadline) }}</div>
              </div>
            </div>
          </div>

          <div class="desc-section">
            <h3>📖 活动详情</h3>
            <div class="desc-content">{{ activity.description || '暂无介绍' }}</div>
          </div>

          <div class="action-bar">
            <div class="registration-count">
              已有 <strong>{{ activity.registeredCount ?? 0 }}</strong> 人报名参加
            </div>
            <div style="display:flex; gap:10px; align-items:center; flex-wrap:wrap;">
              <template v-if="userStore.isStudent">
                <template v-if="myReg">
                  <span style="font-size:14px; color:#666;">您已报名，当前状态：</span>
                  <StatusTag :status="myReg.status" />
                  <button v-if="['registered','approved'].includes(myReg.status)" class="btn btn-danger" :disabled="submitting" @click="handleCancel">取消报名</button>
                </template>
                <template v-else>
                  <button v-if="isJoinable" class="btn btn-primary" :disabled="submitting" @click="handleJoin">{{ submitting ? '提交中...' : '🚀 立即报名参加' }}</button>
                  <span v-else style="font-size:14px; color:#86868b;">{{ unjoinableReason }}</span>
                </template>
              </template>
              <template v-else-if="userStore.isAdmin">
                <button class="btn btn-primary" @click="router.push(`/admin/activities/${activity.id}/edit`)">✏️ 编辑活动</button>
                <button class="btn btn-secondary" @click="router.push({ path: '/admin/registrations', query: { activityId: activity.id } })">📋 查看报名名单</button>
              </template>
            </div>
          </div>
        </div>
      </div>
    </template>
  </div>
</template>