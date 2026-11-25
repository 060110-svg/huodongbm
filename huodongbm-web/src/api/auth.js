import request from '../utils/request'

export function login(username, password) {
  return request.post('/login', { username, password })
}

export function getHome() {
  return request.get('/home')
}

export function getDashboard() {
  return request.get('/dashboard')
}