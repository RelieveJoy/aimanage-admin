import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import * as authApi from '@/api/auth'
import {
  getToken,
  setToken,
  getUser,
  setUser,
  clearAuth,
} from '@/utils/auth-storage'

export const useAuthStore = defineStore('auth', () => {
  // 初始值从 localStorage 恢复，刷新页面不会丢登录态
  const token = ref(getToken() || '')
  const user = ref(getUser())

  const isAdmin = computed(() => user.value?.role === 'ADMIN')
  const displayName = computed(() => user.value?.name || user.value?.username || '')

  async function login(payload) {
    const data = await authApi.login(payload)
    token.value = data.token
    user.value = data.user
    setToken(data.token)
    setUser(data.user)
    return data.user
  }

  /** 刷新页面后若内存里没有 user，用 token 换回来 */
  async function fetchMe() {
    const me = await authApi.getMe()
    user.value = me
    setUser(me)
    return me
  }

  function clear() {
    token.value = ''
    user.value = null
    clearAuth()
  }

  async function logout() {
    try {
      await authApi.logout()
    } catch {
      // JWT 无状态，服务端失败也不影响本地登出
    }
    clear()
  }

  return { token, user, isAdmin, displayName, login, fetchMe, logout, clear }
})
