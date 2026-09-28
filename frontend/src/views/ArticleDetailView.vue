<script setup>
import { computed, nextTick, onMounted, onUnmounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { fetchArticle, fetchArticles, fetchComments, fetchTags, postComment } from '../api'

const route = useRoute()
const router = useRouter()
const article = ref(null)
const comments = ref([])
const progress = ref(0)
const toc = ref([])
const sideList = ref([])
const sideTotal = ref(0)
const sideTitle = ref('本专题文章')
const form = ref({ nickname: '', email: '', content: '' })
const msg = ref('')
const detailMain = ref(null)
const sideRail = ref(null)
const SIDE_SIZE = 80

const contextQuery = computed(() => {
  const q = {}
  if (route.query.tagId) q.tagId = String(route.query.tagId)
  if (route.query.categoryId) q.categoryId = String(route.query.categoryId)
  return q
})

async function load() {
  const res = await fetchArticle(route.params.slug)
  article.value = res.data
  document.title = `${article.value.seoTitle || article.value.title} - 技术实践笔记`
  const meta = document.querySelector('meta[name="description"]') || Object.assign(document.createElement('meta'), { name: 'description' })
  meta.content = article.value.seoDescription || article.value.summary || ''
  if (!meta.parentNode) document.head.appendChild(meta)
  await loadSideList()
  await nextTick()
  if (detailMain.value) detailMain.value.scrollTop = 0
  progress.value = 0
  buildToc()
  enhanceExternalLinks()
  scrollActiveSideItem()
  const c = await fetchComments({ articleId: article.value.id })
  comments.value = c.data.records || []
}

async function loadSideList() {
  const tagId = route.query.tagId ? Number(route.query.tagId) : null
  const categoryId = route.query.categoryId ? Number(route.query.categoryId) : null

  const params = { page: 1, size: SIDE_SIZE }
  if (tagId) {
    params.tagId = tagId
    sideTitle.value = await resolveTagName(tagId)
  } else if (categoryId) {
    params.categoryId = categoryId
    sideTitle.value = article.value?.categoryName ? `专题：${article.value.categoryName}` : '本专题文章'
  } else if (article.value?.categoryId) {
    params.categoryId = article.value.categoryId
    sideTitle.value = article.value.categoryName ? `专题：${article.value.categoryName}` : '本专题文章'
  } else {
    sideList.value = []
    sideTotal.value = 0
    sideTitle.value = '相关文章'
    return
  }

  const res = await fetchArticles(params)
  sideList.value = res.data.records || []
  sideTotal.value = res.data.total || 0
}

async function resolveTagName(tagId) {
  try {
    const t = await fetchTags()
    const hit = (t.data || []).find((x) => x.id === tagId)
    return hit ? `标签：${hit.name}` : '标签文章'
  } catch {
    return '标签文章'
  }
}

function buildToc() {
  const root = document.querySelector('.article-body')
  if (!root) return
  const heads = [...root.querySelectorAll('h2, h3')]
  toc.value = heads.map((h, i) => {
    const id = `heading-${i}`
    h.id = id
    return { id, text: h.textContent, level: h.tagName }
  })
}

function enhanceExternalLinks() {
  const root = document.querySelector('.article-body')
  if (!root) return
  root.querySelectorAll('a[href]').forEach((a) => {
    const href = a.getAttribute('href') || ''
    if (!/^https?:\/\//i.test(href)) return
    a.setAttribute('target', '_blank')
    a.setAttribute('rel', 'noopener noreferrer')
  })
}

function scrollActiveSideItem() {
  nextTick(() => {
    const rail = sideRail.value
    const el = rail?.querySelector('a.is-active')
    if (!rail || !el) return
    const top = el.offsetTop - rail.clientHeight / 2 + el.offsetHeight / 2
    rail.scrollTop = Math.max(0, top)
  })
}

function goBack() {
  if (window.history.state?.back != null) {
    router.back()
    return
  }
  if (route.query.tagId) {
    router.push({ path: '/articles', query: { tagId: String(route.query.tagId) } })
    return
  }
  if (article.value?.categorySlug) {
    router.push(`/categories/${article.value.categorySlug}`)
    return
  }
  router.push('/articles')
}

function onMainScroll() {
  const el = detailMain.value
  if (!el) return
  const max = el.scrollHeight - el.clientHeight
  progress.value = max > 0 ? (el.scrollTop / max) * 100 : 0
}

function onTocClick(e, id) {
  e.preventDefault()
  const target = document.getElementById(id)
  const scroller = detailMain.value
  if (!target || !scroller) return
  const top = target.offsetTop - 12
  scroller.scrollTo({ top, behavior: 'smooth' })
}

async function submit() {
  msg.value = ''
  try {
    await postComment({
      articleId: article.value.id,
      nickname: form.value.nickname,
      email: form.value.email,
      content: form.value.content,
    })
    msg.value = '评论已提交，审核通过后显示。'
    form.value.content = ''
  } catch (e) {
    msg.value = e.message || '提交失败'
  }
}

const formattedDate = computed(() => {
  const v = article.value?.publishedAt
  return v ? String(v).replace('T', ' ').slice(0, 16) : ''
})

const moreLink = computed(() => {
  if (route.query.tagId) {
    return { path: '/articles', query: { tagId: String(route.query.tagId) } }
  }
  if (article.value?.categorySlug) {
    return { path: `/categories/${article.value.categorySlug}` }
  }
  return { path: '/articles' }
})

onMounted(() => {
  load()
})
watch(() => [route.params.slug, route.query.tagId, route.query.categoryId], load)
</script>

<template>
  <div class="article-page">
    <div class="progress" :style="{ width: progress + '%' }"></div>
    <section class="article-detail" v-if="article">
      <div class="article-nav">
        <button type="button" class="back-link" @click="goBack">← 返回上一页</button>
        <RouterLink
          v-if="article.categorySlug"
          class="back-link muted"
          :to="`/categories/${article.categorySlug}`"
        >
          查看专题
        </RouterLink>
        <RouterLink class="back-link muted" to="/articles">全部文章</RouterLink>
      </div>

      <div class="detail-layout">
        <aside class="side-rail" ref="sideRail">
          <div class="side-rail-panel">
            <div class="side-rail-head">
              <strong>{{ sideTitle }}</strong>
              <span class="muted" v-if="sideTotal">{{ Math.min(sideList.length, sideTotal) }}/{{ sideTotal }}</span>
            </div>
            <nav class="side-rail-list">
              <RouterLink
                v-for="item in sideList"
                :key="item.id"
                :to="{ name: 'article', params: { slug: item.slug }, query: contextQuery }"
                :class="{ 'is-active': item.slug === article.slug }"
                :title="item.title"
              >
                {{ item.title }}
              </RouterLink>
            </nav>
            <RouterLink v-if="sideTotal > sideList.length" class="side-rail-more" :to="moreLink">
              查看全部 {{ sideTotal }} 篇 →
            </RouterLink>
          </div>
        </aside>

        <div class="detail-main" ref="detailMain" @scroll="onMainScroll">
          <div class="meta" style="margin-bottom: 0.75rem">
            <RouterLink v-if="article.categorySlug" class="chip" :to="`/categories/${article.categorySlug}`">
              {{ article.categoryName }}
            </RouterLink>
            <span>{{ formattedDate }}</span>
            <span>{{ article.viewCount || 0 }} 阅读</span>
          </div>
          <h1 class="hero-brand article-title">{{ article.title }}</h1>
          <p class="muted">{{ article.summary }}</p>
          <article class="article-body" v-html="article.contentHtml"></article>

          <div class="comment-block">
            <h2 style="font-family: var(--font-serif)">评论</h2>
            <div v-if="!comments.length" class="muted">暂无评论，来做第一个读者吧。</div>
            <div v-for="c in comments" :key="c.id" class="panel" style="margin-top: 0.75rem">
              <div class="meta">
                <strong>{{ c.nickname }}</strong>
                <span>{{ String(c.createdAt || '').replace('T', ' ').slice(0, 16) }}</span>
              </div>
              <p style="margin: 0.4rem 0 0">{{ c.content }}</p>
            </div>
            <form class="comment-form" @submit.prevent="submit">
              <input v-model="form.nickname" required placeholder="昵称" maxlength="32" />
              <input v-model="form.email" type="email" placeholder="邮箱（可选）" />
              <textarea v-model="form.content" required rows="4" placeholder="友善发言，审核后展示" maxlength="1000"></textarea>
              <div>
                <button class="btn" type="submit">提交评论</button>
                <span class="muted" style="margin-left: 0.75rem">{{ msg }}</span>
              </div>
            </form>
          </div>
        </div>

        <aside class="toc-rail">
          <div class="toc" v-if="toc.length">
            <strong>目录</strong>
            <a
              v-for="item in toc"
              :key="item.id"
              :href="`#${item.id}`"
              :style="{ paddingLeft: item.level === 'H3' ? '0.8rem' : 0 }"
              @click="onTocClick($event, item.id)"
            >
              {{ item.text }}
            </a>
          </div>
        </aside>
      </div>
    </section>
  </div>
</template>
