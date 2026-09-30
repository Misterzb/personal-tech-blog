<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import SiteHeader from './components/SiteHeader.vue'
import SiteFooter from './components/SiteFooter.vue'
import { fetchAnnouncements, trackVisit } from './api'

const route = useRoute()
const isArticle = computed(() => route.name === 'article')
const announcements = ref([])
const bannerClosed = ref(sessionStorage.getItem('announcement-closed') === '1')

const activeAnnouncement = computed(() => {
  if (bannerClosed.value || !announcements.value.length) return null
  return announcements.value[0]
})

function closeBanner() {
  bannerClosed.value = true
  sessionStorage.setItem('announcement-closed', '1')
}

onMounted(async () => {
  trackVisit(route.fullPath).catch(() => {})
  try {
    const res = await fetchAnnouncements()
    announcements.value = (res.data?.records || res.data || []).filter((a) => a.status !== 0 && a.enabled !== false)
  } catch (_) {}
})

watch(
  () => route.fullPath,
  (path) => {
    trackVisit(path).catch(() => {})
  }
)
</script>

<template>
  <div class="app-shell" :class="{ 'is-article': isArticle }">
    <div v-if="activeAnnouncement" class="announce-banner">
      <div class="container announce-inner">
        <strong>{{ activeAnnouncement.title }}</strong>
        <span>{{ activeAnnouncement.content || activeAnnouncement.body }}</span>
        <a
          v-if="activeAnnouncement.link"
          :href="activeAnnouncement.link"
          target="_blank"
          rel="noopener"
        >详情</a>
        <button type="button" class="announce-close" aria-label="关闭" @click="closeBanner">×</button>
      </div>
    </div>
    <SiteHeader />
    <main class="app-main">
      <RouterView />
    </main>
    <SiteFooter v-if="!isArticle" />
  </div>
</template>
