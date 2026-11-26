<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getMyRegistrations, cancelRegistration } from '../api/registration'
import StatusTag from '../components/StatusTag.vue'
import { fmtDate } from '../utils/format'
import { useToast } from '../utils/toast'
import { confirm } from '../utils/confirm'

const router = useRouter()
const toast = useToast()
const list = ref([])
const statusFilter = ref('')
const loading = ref(true)

// 报名状态分类筛选（值与后端 status 字段一致）
const STATUS_OPTIONS = [
  ['', '🎯 全部状态'],
  ['registered', '⏳ 待审核'],
  ['approved', '✅ 已通过'],
  ['rejected', '❌ 已拒绝'],
  ['cancelled', '🚫 已取消'],
]

async function load() {
  loading.value = true
  try {
    const params = {}
    if (statusFilter.value) params.status = statusFilter.value
    const res = await getMyRegistrations(params)
    list.value = res.data || []
  } catch (e) {
    toast.error(e.message)
  } finally {
    loading.value = false
  }
}
onMounted(load)

async function handleCancel(r) {
  const ok = await confirm({ title: '取消报名', message: `确定取消「${r.activityTitle}」的报名吗？\n取消后需要重新报名才能参加。`, okText: '确认取消' })
  if (!ok) return
  try {
    await cancelRegistration(r.id)
    toast.success('已取消报名')
    load()
  } catch (e) {
    toast.error(e.message)
  }
}
</script>

<template>
  <div class="container">
    <div class="page-hero hero-pink">
      <div class="page-hero-content">
        <h2>🎫 我的报名</h2>
        <p>查看和管理你报名的所有精彩活动～</p>
      </div>
    </div>

    <div class="toolbar">
      <div class="filter-group">
        <select v-model="statusFilter" @change="load" aria-label="按报名状态筛选">
          <option v-for="[value, label] in STATUS_OPTIONS" :key="value" :value="value">{{ label }}</option>
        </select>
      </div>
    </div>

    <div v-if="loading" class="view-loading">加载中...</div>
    <template v-else>
      <table v-if="list.length">
        <thead>
          <tr>
            <th>报名时间</th>
            <th>活动标题</th>
            <th>状态</th>
            <th>备注</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="r in list" :key="r.id">
            <td>{{ fmtDate(r.registerTime) }}</td>
            <td style="font-weight:600;color:#1d1d1f;">{{ r.activityTitle || '-' }}</td>
            <td><StatusTag :status="r.status" /></td>
            <td>{{ r.remark || '-' }}</td>
            <td>
              <a style="color:#667eea;text-decoration:none;margin-right:14px;font-weight:600;" @click="router.push(`/activities/${r.activityId}`)">查看活动</a>
              <button v-if="['registered','approved'].includes(r.status)" class="cancel-reg-btn" @click="handleCancel(r)">取消报名</button>
              <a v-if="['rejected','cancelled'].includes(r.status)" style="color:#667eea;text-decoration:none;margin-right:14px;font-weight:600;" @click="router.push(`/activities/${r.activityId}`)">重新报名</a>
            </td>
          </tr>
        </tbody>
      </table>

      <div v-else class="empty">
        <div class="icon bounce">🎯</div>
        <h3>还没有报名任何活动</h3>
        <p>去活动广场看看有什么感兴趣的吧～</p>
        <button class="btn btn-primary" @click="router.push('/activities')">🚀 前往活动广场</button>
      </div>
    </template>
  </div>
</template>