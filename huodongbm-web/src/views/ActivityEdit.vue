<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getActivityDetail, createActivity, updateActivity } from '../api/activity'
import { useToast } from '../utils/toast'

const route = useRoute()
const router = useRouter()
const toast = useToast()

const isEdit = computed(() => !!route.params.id)
const id = computed(() => Number(route.params.id))

const form = ref({
  title: '',
  description: '',
  location: '',
  startTime: '',
  endTime: '',
  deadline: '',
  maxParticipants: 0,
  status: 'published',
})
const coverFile = ref(null)
const coverPreview = ref('')
const submitting = ref(false)
const loading = ref(true)

function toLocalInput(s) {
  if (!s) return ''
  return String(s).slice(0, 16).replace(' ', 'T')
}

onMounted(async () => {
  if (!isEdit.value) { loading.value = false; return }
  try {
    const res = await getActivityDetail(id.value)
    const a = res.data
    form.value = {
      title: a.title || '',
      description: a.description || '',
      location: a.location || '',
      startTime: toLocalInput(a.startTime),
      endTime: toLocalInput(a.endTime),
      deadline: toLocalInput(a.deadline),
      maxParticipants: a.maxParticipants || 0,
      status: a.status || 'published',
    }
    coverPreview.value = a.coverImage || ''
  } catch (e) {
    toast.error(e.message)
  } finally {
    loading.value = false
  }
})

function onCoverChange(e) {
  const f = e.target.files?.[0]
  if (!f) return
  if (!/\.(jpe?g|png|gif|webp)$/i.test(f.name)) {
    toast.error('仅支持 JPG / JPEG / PNG / GIF / WEBP 格式')
    return
  }
  if (f.size > 10 * 1024 * 1024) {
    toast.error('封面图大小不能超过 10MB')
    return
  }
  coverFile.value = f
  coverPreview.value = URL.createObjectURL(f)
}

function clearCover() {
  coverFile.value = null
  coverPreview.value = ''
}

function validate() {
  if (!form.value.title.trim()) return '活动标题不能为空'
  if (form.value.title.trim().length > 100) return '活动标题不能超过100个字符'
  if (!form.value.startTime) return '请选择开始时间'
  if (!form.value.endTime) return '请选择结束时间'
  if (form.value.startTime && form.value.endTime && form.value.startTime >= form.value.endTime) {
    return '结束时间必须晚于开始时间'
  }
  if (form.value.deadline && form.value.startTime && form.value.deadline >= form.value.startTime) {
    return '报名截止时间必须早于开始时间'
  }
  return null
}

async function onSubmit() {
  const err = validate()
  if (err) { toast.error(err); return }
  submitting.value = true
  try {
    const fd = new FormData()
    fd.append('title', form.value.title.trim())
    fd.append('description', form.value.description)
    fd.append('location', form.value.location)
    fd.append('startTime', form.value.startTime)
    fd.append('endTime', form.value.endTime)
    if (form.value.deadline) fd.append('deadline', form.value.deadline)
    fd.append('maxParticipants', Number(form.value.maxParticipants) || 0)
    fd.append('status', form.value.status)
    if (!isEdit.value) fd.append('currentParticipants', 0)
    if (coverFile.value) fd.append('coverFile', coverFile.value)

    if (isEdit.value) {
      await updateActivity(id.value, fd)
      toast.success('活动更新成功')
    } else {
      await createActivity(fd)
      toast.success('活动发布成功')
    }
    router.push('/activities')
  } catch (e) {
    toast.error(e.message)
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <div class="container-narrow">
    <button v-if="loading" class="view-loading">加载中...</button>
    <template v-else>
      <div class="page-hero hero-primary">
        <div class="page-hero-content">
          <h2>{{ isEdit ? '✏️ 编辑活动' : '➕ 发布新活动' }}</h2>
          <p>{{ isEdit ? '修改活动信息，更新后将实时生效。' : '创建一场精彩的校园活动，邀请同学们一起参加！' }}</p>
        </div>
      </div>

      <div class="form-card">
        <form @submit.prevent="onSubmit">
          <div class="form-item">
            <label>活动标题<span class="required">*</span></label>
            <input v-model="form.title" maxlength="100" placeholder="请输入活动名称" />
          </div>

          <div class="form-item">
            <label>活动描述</label>
            <textarea v-model="form.description" rows="5" placeholder="详细描述活动内容、流程、注意事项等"></textarea>
          </div>

          <div class="form-item">
            <label>活动地点</label>
            <input v-model="form.location" maxlength="100" placeholder="如：教学楼A301" />
          </div>

          <div class="form-row">
            <div class="form-item">
              <label>开始时间<span class="required">*</span></label>
              <input v-model="form.startTime" type="datetime-local" />
            </div>
            <div class="form-item">
              <label>结束时间<span class="required">*</span></label>
              <input v-model="form.endTime" type="datetime-local" />
            </div>
          </div>

          <div class="form-row">
            <div class="form-item">
              <label>报名截止</label>
              <input v-model="form.deadline" type="datetime-local" />
            </div>
            <div class="form-item">
              <label>最大人数</label>
              <input v-model.number="form.maxParticipants" type="number" min="0" placeholder="0 表示不限" />
            </div>
          </div>

          <div class="form-item">
            <label>活动状态</label>
            <select v-model="form.status">
              <option value="published">🔥 已发布（可报名）</option>
              <option value="ongoing">🚀 进行中</option>
              <option value="ended">✅ 已结束</option>
              <option value="offline">🚫 已下线</option>
            </select>
          </div>

          <div class="form-item">
            <label>封面图</label>
            <div class="upload-area">
              <img v-if="coverPreview" :src="coverPreview" class="cover-preview" />
              <div v-else style="font-size:36px;margin-bottom:8px;">🖼️</div>
              <div style="font-weight:600;margin-bottom:6px;">{{ coverPreview ? '点击更换封面图片' : '点击或拖拽上传封面图片' }}</div>
              <div style="font-size:12px;color:#86868b;margin-bottom:12px;">支持 JPG / PNG / GIF / WEBP，最大 10MB</div>
              <input type="file" accept="image/jpeg,image/png,image/gif,image/webp" @change="onCoverChange" />
              <button v-if="coverPreview" type="button" class="btn btn-secondary" style="margin-top:10px;" @click="clearCover">✕ 移除封面</button>
            </div>
          </div>

          <div class="form-actions">
            <button type="button" class="btn btn-secondary" @click="router.back()">← 取消返回</button>
            <button type="submit" class="btn btn-primary" :disabled="submitting">{{ submitting ? '提交中...' : (isEdit ? '💾 保存修改' : '🚀 立即发布') }}</button>
          </div>
        </form>
      </div>
    </template>
  </div>
</template>

<style scoped>
.cover-preview { max-width: 220px; max-height: 140px; border-radius: 8px; margin-bottom: 10px; object-fit: cover; border: 1px solid #eee; }
</style>