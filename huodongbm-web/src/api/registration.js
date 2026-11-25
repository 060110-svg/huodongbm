import request from '../utils/request'

export function joinRegistration(activityId) {
  return request.post('/registrations/join', null, { params: { activityId } })
}

export function getMyRegistrations() {
  return request.get('/registrations/my')
}

export function cancelRegistration(id) {
  return request.post(`/registrations/${id}/cancel`)
}

export function getRegistrationList(params) {
  return request.get('/registrations', { params })
}

export function approveRegistration(id) {
  return request.post(`/registrations/${id}/approve`)
}

export function rejectRegistration(id, remark) {
  return request.post(`/registrations/${id}/reject`, null, { params: { remark } })
}

export function reauditRegistration(id) {
  return request.post(`/registrations/${id}/reaudit`)
}