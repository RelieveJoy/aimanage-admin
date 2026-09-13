import axios from 'axios'
import { ElMessage } from 'element-plus'
import { getToken, clearAuth } from '@/utils/auth-storage'

const request = axios.create({
  // 一律使用相对路径，由 vite proxy（开发）/ nginx（生产）转发到后端
  baseURL: '/api',
  timeout: 15000,
})

/* ---------------- 请求拦截：带上 token ---------------- */
request.interceptors.request.use((config) => {
  const token = getToken()
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

/* ---------------- 响应拦截：拆包 + 统一错误 ---------------- */
request.interceptors.response.use(
  (response) => {
    const body = response.data
    // 后端统一响应体 {code, msg, data}，成功时直接把 data 交给调用方
    if (body && body.code === 0) {
      return body.data
    }
    ElMessage.error(body?.msg || '请求失败')
    return Promise.reject(new Error(body?.msg || '请求失败'))
  },
  async (error) => {
    const status = error.response?.status
    const msg = error.response?.data?.msg

    if (status === 401) {
      clearAuth()
      ElMessage.error(msg || '登录已过期，请重新登录')
      // 动态 import 打断循环依赖（见 utils/auth-storage.js 注释）
      const router = (await import('@/router')).default
      router.replace({
        name: 'login',
        query: { redirect: router.currentRoute.value.fullPath },
      })
    } else if (status === 403) {
      ElMessage.error(msg || '无权访问该功能')
    } else if (status === 400 || status === 409) {
      ElMessage.error(msg || '操作失败')
    } else {
      ElMessage.error(msg || '网络异常，请稍后重试')
    }

    return Promise.reject(error)
  },
)

export default request
