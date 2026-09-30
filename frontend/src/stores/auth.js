import { defineStore } from 'pinia'
import { computed, ref } from 'vue'
import { fetchMe, loginMember, registerMember } from '../api'

const TOKEN_KEY = 'member_token'
const MEMBER_KEY = 'member_profile'

export const useAuthStore = defineStore('auth', () => {
  const token = ref(localStorage.getItem(TOKEN_KEY) || '')
  const member = ref(safeParse(localStorage.getItem(MEMBER_KEY)))

  const isLoggedIn = computed(() => Boolean(token.value))

  function persist() {
    if (token.value) localStorage.setItem(TOKEN_KEY, token.value)
    else localStorage.removeItem(TOKEN_KEY)
    if (member.value) localStorage.setItem(MEMBER_KEY, JSON.stringify(member.value))
    else localStorage.removeItem(MEMBER_KEY)
  }

  function setSession(data) {
    token.value = data.token || ''
    member.value = {
      id: data.id,
      phone: data.phone,
      nickname: data.nickname,
      email: data.email || '',
      avatar: data.avatar || '',
    }
    persist()
  }

  async function register(payload) {
    const res = await registerMember(payload)
    setSession(res.data)
    return res.data
  }

  async function login(payload) {
    const res = await loginMember(payload)
    setSession(res.data)
    return res.data
  }

  async function refreshMe() {
    if (!token.value) return null
    try {
      const res = await fetchMe()
      member.value = res.data
      persist()
      return res.data
    } catch {
      logout()
      return null
    }
  }

  function logout() {
    token.value = ''
    member.value = null
    persist()
  }

  return {
    token,
    member,
    isLoggedIn,
    register,
    login,
    refreshMe,
    logout,
  }
})

function safeParse(raw) {
  if (!raw) return null
  try {
    return JSON.parse(raw)
  } catch {
    return null
  }
}
