import axios from 'axios'
import { ElMessage } from 'element-plus'
import { decryptApiData } from '../utils/apiCrypto'

const http = axios.create({
  baseURL: '',
  timeout: 20000,
})

http.interceptors.request.use((config) => {
  const token = localStorage.getItem('blog_token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

http.interceptors.response.use(
  async (res) => {
    const body = res.data
    if (body && typeof body.code === 'number' && body.code !== 0) {
      ElMessage.error(body.message || '请求失败')
      return Promise.reject(new Error(body.message || '请求失败'))
    }
    if (body && body.encrypted && typeof body.data === 'string') {
      try {
        body.data = await decryptApiData(body.data)
        body.encrypted = false
      } catch (e) {
        ElMessage.error('响应解密失败，请检查 VITE_API_AES_KEY 是否与后端一致')
        return Promise.reject(new Error('响应解密失败'))
      }
    }
    return body
  },
  (err) => {
    if (err.response?.status === 401 || err.response?.status === 403) {
      localStorage.removeItem('blog_token')
      if (!location.hash.includes('/login') && !location.pathname.includes('/login')) {
        location.href = '/admin/#/login'
      }
    }
    ElMessage.error(err.response?.data?.message || err.message || '网络错误')
    return Promise.reject(err)
  }
)

export default http
