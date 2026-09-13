import request from './request'

/** 登录 —— 复用后端共用接口，Admin 端只是自己有一个登录页 */
export function login(data) {
  return request.post('/auth/login', data)
}

export function logout() {
  return request.post('/auth/logout')
}

/** 拉取当前登录用户，用于刷新页面后恢复状态 */
export function getMe() {
  return request.get('/auth/me')
}
