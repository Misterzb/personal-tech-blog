import { computed, ref, watch } from 'vue'
import { defineStore } from 'pinia'

const THEME_KEY = 'blog-theme'
const FONT_KEY = 'blog-font-size'

function readTheme() {
  const v = localStorage.getItem(THEME_KEY)
  return v === 'dark' ? 'dark' : 'light'
}

function readFontSize() {
  const v = localStorage.getItem(FONT_KEY)
  return v === 'sm' || v === 'lg' ? v : 'md'
}

export const useThemeStore = defineStore('theme', () => {
  const theme = ref(readTheme())
  const fontSize = ref(readFontSize())

  const isDark = computed(() => theme.value === 'dark')

  function apply() {
    const root = document.documentElement
    root.setAttribute('data-theme', theme.value)
    root.setAttribute('data-font-size', fontSize.value)
    localStorage.setItem(THEME_KEY, theme.value)
    localStorage.setItem(FONT_KEY, fontSize.value)
  }

  function toggleTheme() {
    theme.value = theme.value === 'dark' ? 'light' : 'dark'
  }

  function setFontSize(size) {
    if (size === 'sm' || size === 'md' || size === 'lg') {
      fontSize.value = size
    }
  }

  function cycleFontSize() {
    const order = ['sm', 'md', 'lg']
    const i = order.indexOf(fontSize.value)
    fontSize.value = order[(i + 1) % order.length]
  }

  watch([theme, fontSize], apply, { immediate: true })

  return {
    theme,
    fontSize,
    isDark,
    toggleTheme,
    setFontSize,
    cycleFontSize,
    apply,
  }
})
