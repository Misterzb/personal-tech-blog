import axios from 'axios'
import { decryptApiData } from '../utils/apiCrypto'

const http = axios.create({
  baseURL: '',
  timeout: 15000,
})

http.interceptors.request.use((config) => {
  const token = localStorage.getItem('member_token')
  if (token) {
    config.headers = config.headers || {}
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

http.interceptors.response.use(
  async (res) => {
    const body = res.data
    if (body && typeof body.code === 'number' && body.code !== 0) {
      return Promise.reject(new Error(body.message || '请求失败'))
    }
    if (body && body.encrypted && typeof body.data === 'string') {
      try {
        body.data = await decryptApiData(body.data)
        body.encrypted = false
      } catch (e) {
        return Promise.reject(new Error('响应解密失败'))
      }
    }
    return body
  },
  (err) => {
    const msg = err.response?.data?.message || err.message || '请求失败'
    return Promise.reject(new Error(msg))
  }
)

export default http
