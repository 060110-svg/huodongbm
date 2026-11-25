<script setup>
import { ref, watch, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getActivityList, deleteActivity } from '../api/activity'
import { useUserStore } from '../stores/user'
import StatusTag from '../components/StatusTag.vue'
import { fmtDate } from '../utils/format'
import { useToast } from '../utils/toast'
import { confirm } from '../utils/confirm'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const toast = useToast()

const list = ref([])
const total = ref(0)
const currentPage = ref(1)
const totalPages = ref(0)
const pageStart = ref(1)
const pageEnd = ref(1)
const keyword = ref(route.query.title || '')
const loading = ref(true)

async function load() {
  loading.value = true
  try {
    const params = { page: route.query.page || 1 }
    if (route.query.title) params.title = route.query.title
    const res = await getActivityList(params)
    const d = res.data
    list.value = d.list
    total.value = d.total
    currentPage.value = d.currentPage
    totalPages.value = d.totalPages
    pageStart.value = d.pageStart
    pageEnd.value = d.pageEnd
  } catch (e) {
    toast.error(e.message)
  } finally {
    loading.value = false
  }
}

onMounted(load)
watch(() => route.query, load)

function doSearch() {
  const query = {}
  if (keyword.value.trim()) query.title = keyword.value.trim()
  router.push({ path: route.path, query })
}

function goPage(p) {
  if (p < 1 || p > totalPages.value || p === currentPage.value) return
  const query = { ...route.query, page: p }
  router.push({ path: route.path, query })
}

async function handleDelete(a) {
  const ok = await confirm({ title: '确认删除', message: `确定删除活动「${a.title}」吗？\n此操作无法撤销，相关报名记录也会被移除。`, okText: '删除' })
  if (!ok) return
  try {
    await deleteActivity(a.id)
    toast.success('删除成功')
    load()
  } catch (e) {
    toast.error(e.message)
  }
}

const pageNumbers = () => {
  const arr = []
  for (let i = pageStart.value; i <= pageEnd.value; i++) arr.push(i)
  return arr
}
</script>

<template>
  <div class="container">
    <div class="page-hero hero-primary">
      <div class="page-hero-content">
        <h2>📅 活动管理</h2>
        <p>发布、管理、查看校园活动详情，让活动组织更高效！</p>
      </div>
    </div>

    <div class="toolbar">
      <div class="search-box">
        <input v-model="keyword" @keyup.enter="doSearch" placeholder="搜索感兴趣的活动..." />
        <button class="btn btn-primary" @click="doSearch">搜索</button>
        <button v-if="keyword" class="btn btn-secondary" @click="keyword=''; router.push(route.path)">✕ 重置</button>
      </div>
      <button v-if="userStore.isAdmin" class="btn btn-primary" @click="router.push('/admin/activities/new')">➕ 发布新活动</button>
    </div>

    <div v-if="loading" class="view-loading">加载中...</div>
    <template v-else>
      <div v-if="list.length" class="activity-grid">
        <div v-for="a in list" :key="a.id" class="activity-card" @click="router.push(`/activities/${a.id}`)">
          <img v-if="a.coverImage" :src="a.coverImage" class="card-cover-img" />
          <div v-else class="card-cover" :class="'c' + ((a.id % 6) + 1)">{{ a.title ? a.title.charAt(0) : '' }}</div>
          <div class="card-body">
            <div class="card-title">{{ a.title }}</div>
            <div class="card-meta">
              <span>📍 {{ a.location || '待定' }}</span>
              <span>🕐 {{ fmtDate(a.startTime) }}</span>
              <span>👥 {{ a.maxParticipants > 0 ? '限 ' + a.maxParticipants + ' 人' : '不限人数' }}</span>
            </div>
            <div class="card-footer">
              <div class="registration-info">
                已报名 <strong>{{ a.registeredCount ?? 0 }}</strong> 人
                <StatusTag :status="a.status" />
              </div>
              <div class="actions" @click.stop>
                <a class="btn-detail" @click="router.push(`/activities/${a.id}`)">查看详情</a>
                <template v-if="userStore.isAdmin">
                  <a class="btn-edit" @click="router.push(`/admin/activities/${a.id}/edit`)">编辑</a>
                  <button class="btn-delete" @click="handleDelete(a)">删除</button>
                </template>
              </div>
            </div>
          </div>
        </div>
      </div>
      <div v-else class="empty">
        <div class="icon">📭</div>
        <h3>暂时没有找到活动</h3>
        <p>换个关键词试试吧～</p>
        <button v-if="userStore.isAdmin" class="btn btn-primary" @click="router.push('/admin/activities/new')">去发布第一个活动 →</button>
      </div>

      <div v-if="totalPages > 1" class="pagination">
        <button :class="{ disabled: currentPage === 1 }" :disabled="currentPage === 1" @click="goPage(1)">« 首页</button>
        <button :class="{ disabled: currentPage === 1 }" :disabled="currentPage === 1" @click="goPage(currentPage - 1)">‹ 上一页</button>
        <button v-for="i in pageNumbers()" :key="i" :class="{ current: i === currentPage }" @click="goPage(i)">{{ i }}</button>
        <button :class="{ disabled: currentPage === totalPages }" :disabled="currentPage === totalPages" @click="goPage(currentPage + 1)">下一页 ›</button>
        <button :class="{ disabled: currentPage === totalPages }" :disabled="currentPage === totalPages" @click="goPage(totalPages)">末页 »</button>
        <span class="info">共 {{ total }} 条 · {{ totalPages }} 页</span>
      </div>
    </template>
  </div>
</template>