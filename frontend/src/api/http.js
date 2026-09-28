import axios from 'axios'

const http = axios.create({
  baseURL: '',
  timeout: 15000,
})

http.interceptors.response.use(
  (res) => {
    const body = res.data
    if (body && typeof body.code === 'number' && body.code !== 0) {
      return Promise.reject(new Error(body.message || '请求失败'))
    }
    return body
  },
  (err) => Promise.reject(err)
)

export default http
