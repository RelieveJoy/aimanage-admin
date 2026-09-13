/**
 * 登录态本地存储。
 *
 * 单独抽成模块，是为了打断循环依赖：
 *   router → stores/auth → api/auth → request → 本模块
 * request.js 需要读 token，若它去 import pinia store 就会形成环。
 */
const TOKEN_KEY = 'aimanage_admin_token'
const USER_KEY = 'aimanage_admin_user'

export function getToken() {
  return localStorage.getItem(TOKEN_KEY)
}

export function setToken(token) {
  localStorage.setItem(TOKEN_KEY, token)
}

export function getUser() {
  try {
    return JSON.parse(localStorage.getItem(USER_KEY))
  } catch {
    return null
  }
}

export function setUser(user) {
  localStorage.setItem(USER_KEY, JSON.stringify(user))
}

export function clearAuth() {
  localStorage.removeItem(TOKEN_KEY)
  localStorage.removeItem(USER_KEY)
}
