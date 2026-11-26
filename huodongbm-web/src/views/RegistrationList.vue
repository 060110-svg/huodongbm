<script setup>
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getRegistrationList, approveRegistration, rejectRegistration, reauditRegistration } from '../api/registration'
import StatusTag from '../components/StatusTag.vue'
import { fmtDate } from '../utils/format'
import { useToast } from '../utils/toast'
import { confirm } from '../utils/confirm'

const route = useRoute()
const router = useRouter()
const toast = useToast()

const list = ref([])
const activities = ref([])
const currentActivityId = ref(route.query.activityId ? Number(route.query.activityId) : null)
const statusFilter = ref(route.query.status || '')
const loading = ref(true)

// 报名状态分类筛选（值与后端 status 字段一致）
const STATUS_OPTIONS = [
  ['', '🎯 全部状态'],
  ['registered', '⏳ 待审核'],
  ['approved', '✅ 已通过'],
  ['rejected', '❌ 已拒绝'],
  ['cancelled', '🚫 已取消'],
]

const rejectVisible = ref(false)
const rejectTarget = ref(null)
const rejectReason = ref('')

async function load() {
  loading.value = true
  try {
    const params = {}
    if (currentActivityId.value) params.activityId = currentActivityId.value
    if (statusFilter.value) params.status = statusFilter.value
    const res = await getRegistrationList(params)
    list.value = res.data.list || []
    activities.value = res.data.activities || []
  } catch (e) {
    toast.error(e.message)
  } finally {
    loading.value = false
  }
}
onMounted(load)

function onFilterChange() {
  const query = {}
  if (currentActivityId.value) query.activityId = currentActivityId.value
  if (statusFilter.value) query.status = statusFilter.value
  router.replace({ path: '/admin/registrations', query })
  load()
}

async function handleApprove(r) {
  const ok = await confirm({ title: '通过审核', message: `确定通过「${r.realName || r.username}」对「${r.activityTitle}」的报名吗？`, okText: '通过', okClass: 'success' })
  if (!ok) return
  try {
    await approveRegistration(r.id)
    toast.success('已通过审核')
    load()
  } catch (e) {
    toast.error(e.message)
  }
}

function openReject(r) {
  rejectTarget.value = r
  rejectReason.value = ''
  rejectVisible.value = true
}
async function submitReject() {
  if (!rejectTarget.value) return
  try {
    await rejectRegistration(rejectTarget.value.id, rejectReason.value)
    toast.success('已拒绝报名')
    rejectVisible.value = false
    load()
  } catch (e) {
    toast.error(e.message)
  }
}

async function handleReaudit(r) {
  const ok = await confirm({ title: '重新审核', message: `将「${r.realName || r.username}」的报名重置为待审核状态？`, okText: '重置', okClass: 'primary' })
  if (!ok) return
  try {
    await reauditRegistration(r.id)
    toast.success('已重置为待审核')
    load()
  } catch (e) {
    toast.error(e.message)
  }
}
</script>

<template>
  <div class="container">
    <div class="page-hero hero-blue">
      <div class="page-hero-content">
        <h2>📋 报名管理</h2>
        <p>审核学生的报名申请，管理所有报名记录～</p>
      </div>
    </div>

    <div class="toolbar">
      <div class="filter-group">
        <select v-model.number="currentActivityId" @change="onFilterChange" aria-label="按活动筛选">
          <option :value="null">🎯 全部活动</option>
          <option v-for="a in activities" :key="a.id" :value="a.id">{{ a.title }}</option>
        </select>
        <select v-model="statusFilter" @change="onFilterChange" aria-label="按报名状态筛选">
          <option v-for="[value, label] in STATUS_OPTIONS" :key="value" :value="value">{{ label }}</option>
        </select>
      </div>
    </div>

    <div v-if="loading" class="view-loading">加载中...</div>
    <template v-else>
      <table v-if="list.length">
        <thead>
          <tr>
            <th>ID</th>
            <th>报名用户</th>
            <th>活动名称</th>
            <th>报名时间</th>
            <th>状态</th>
            <th>备注</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="r in list" :key="r.id">
            <td>{{ r.id }}</td>
            <td>
              <div style="font-weight:600;color:#1d1d1f;">{{ r.realName || '-' }}</div>
              <div style="font-size:12px;color:#86868b;">@{{ r.username }}</div>
            </td>
            <td style="font-weight:600;">{{ r.activityTitle }}</td>
            <td>{{ fmtDate(r.registerTime) }}</td>
            <td><StatusTag :status="r.status" /></td>
            <td>{{ r.remark || '-' }}</td>
            <td>
              <template v-if="r.status === 'registered'">
                <button class="btn btn-success" style="padding:7px 16px;font-size:12px;margin-right:8px;" @click="handleApprove(r)">✓ 通过</button>
                <button class="btn btn-danger" style="padding:7px 16px;font-size:12px;" @click="openReject(r)">✗ 拒绝</button>
              </template>
              <template v-else-if="r.status === 'approved'">
                <button class="btn btn-danger" style="padding:7px 16px;font-size:12px;margin-right:8px;" @click="openReject(r)">✗ 拒绝</button>
                <button class="btn btn-primary" style="padding:7px 16px;font-size:12px;" @click="handleReaudit(r)">🔄 重新审核</button>
              </template>
              <template v-else-if="r.status === 'rejected'">
                <button class="btn btn-success" style="padding:7px 16px;font-size:12px;margin-right:8px;" @click="handleApprove(r)">✓ 通过</button>
                <button class="btn btn-primary" style="padding:7px 16px;font-size:12px;" @click="handleReaudit(r)">🔄 重新审核</button>
              </template>
              <template v-else>
                <span style="color:#ccc;">—</span>
              </template>
            </td>
          </tr>
        </tbody>
      </table>

      <div v-else class="empty">
        <div class="icon">📭</div>
        <h3>暂无报名记录</h3>
        <p>当有学生报名活动后，这里会显示报名信息～</p>
      </div>
    </template>

    <div v-if="rejectVisible" class="modal-mask active" @click.self="rejectVisible=false">
      <div class="modal">
        <h3>❌ 拒绝报名</h3>
        <p style="color:#86868b;margin:10px 0 20px;">拒绝「{{ rejectTarget?.realName || rejectTarget?.username }}」对「{{ rejectTarget?.activityTitle }}」的报名申请</p>
        <label>拒绝原因（选填，将展示在学生的报名记录中）</label>
        <textarea v-model="rejectReason" rows="3" maxlength="255" placeholder="请输入拒绝原因（不超过255字）"></textarea>
        <div class="modal-actions">
          <button class="btn btn-secondary" @click="rejectVisible=false">取消</button>
          <button class="btn btn-danger" @click="submitReject">确认拒绝</button>
        </div>
      </div>
    </div>
  </div>
</template>