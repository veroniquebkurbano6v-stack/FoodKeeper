import axios from 'axios'
import type { AxiosInstance, AxiosRequestConfig, AxiosResponse } from 'axios'
import { ElMessage } from 'element-plus'
import router from '@/router'
import { useUserStore } from '@/store/user'

const service: AxiosInstance = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL,
  timeout: 10000,
  headers: {
    'Content-Type': 'application/json;charset=utf-8'
  }
})

service.interceptors.request.use(
  (config: AxiosRequestConfig) => {
    const userStore = useUserStore()
    if (userStore.token) {
      config.headers = config.headers || {}
      config.headers.Authorization = `Bearer ${userStore.token}`
    }
    return config
  },
  (error) => {
    return Promise.reject(error)
  }
)

service.interceptors.response.use(
  (response: AxiosResponse) => {
    const { code, message } = response.data
    if (code === 200) {
      return response.data
    } else if (code === 3000 || code === 3002) {
      ElMessage.error(message || '登录已过期')
      router.push('/login')
      return Promise.reject(new Error(message))
    } else {
      ElMessage.error(message || '请求失败')
      return Promise.reject(new Error(message))
    }
  },
  (error) => {
    if (error.message === 'Network Error') {
      console.error('网络连接失败:', error)
    } else {
      ElMessage.error(error.message || '网络错误')
    }
    return Promise.reject(error)
  }
)

export default service