<script setup>
import { computed, onMounted, watch } from 'vue'
import { useRoute } from 'vue-router'
import SiteHeader from './components/SiteHeader.vue'
import SiteFooter from './components/SiteFooter.vue'
import { trackVisit } from './api'

const route = useRoute()
const isArticle = computed(() => route.name === 'article')

onMounted(() => {
  trackVisit(route.fullPath).catch(() => {})
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
    <SiteHeader />
    <main class="app-main">
      <RouterView />
    </main>
    <SiteFooter v-if="!isArticle" />
  </div>
</template>
