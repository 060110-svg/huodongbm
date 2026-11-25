<script setup>
const props = defineProps({ status: { type: String, default: '' } })

const map = {
  published: { text: '🔥 可报名', cls: 'status-published' },
  ongoing: { text: '🚀 进行中', cls: 'status-ongoing' },
  ended: { text: '✅ 已结束', cls: 'status-ended' },
  offline: { text: '🚫 已下线', cls: 'status-offline' },
  registered: { text: '⏳ 待审核', cls: 'status-registered' },
  approved: { text: '✅ 已通过', cls: 'status-approved' },
  rejected: { text: '❌ 已拒绝', cls: 'status-rejected' },
  cancelled: { text: '🚫 已取消', cls: 'status-cancelled' },
}
</script>

<template>
  <span v-if="map[status]" class="status-tag" :class="map[status].cls">{{ map[status].text }}</span>
  <span v-else class="status-tag">{{ status }}</span>
</template>