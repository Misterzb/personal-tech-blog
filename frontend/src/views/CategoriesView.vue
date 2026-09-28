<script setup>
import { onMounted, ref } from 'vue'
import { fetchCategories } from '../api'

const categories = ref([])

onMounted(async () => {
  const res = await fetchCategories()
  categories.value = res.data || []
  document.title = '专题 - 技术实践笔记'
})
</script>

<template>
  <section class="section container">
    <div class="section-head">
      <div>
        <h2>专题</h2>
        <p class="muted">按专项聚合，覆盖 Java、Agent、Python、小程序与 Web。</p>
      </div>
    </div>
    <div class="grid-3">
      <RouterLink v-for="c in categories" :key="c.id" class="panel" :to="`/categories/${c.slug}`">
        <h3>{{ c.name }}</h3>
        <p class="muted" style="margin: 0">{{ c.description }}</p>
      </RouterLink>
    </div>
  </section>
</template>
