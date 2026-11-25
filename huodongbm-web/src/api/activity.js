import request from '../utils/request'

export function getActivityList(params) {
  return request.get('/activities', { params })
}

export function getActivityDetail(id) {
  return request.get(`/activities/${id}`)
}

export function createActivity(formData) {
  return request.post('/activities', formData)
}

export function updateActivity(id, formData) {
  return request.put(`/activities/${id}`, formData)
}

export function deleteActivity(id) {
  return request.delete(`/activities/${id}`)
}