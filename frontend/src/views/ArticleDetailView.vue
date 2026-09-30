<script setup>
import { computed, nextTick, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  addFavorite,
  fetchArticle,
  fetchArticles,
  fetchCategoryToc,
  fetchComments,
  fetchFavorites,
  fetchTags,
  postComment,
  removeFavorite,
  saveReadingProgress,
} from '../api'
import { useAuthStore } from '../stores/auth'
import CaptchaField from '../components/CaptchaField.vue'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const article = ref(null)
const comments = ref([])
const progress = ref(0)
const toc = ref([])
const categoryToc = ref([])
const sideList = ref([])
const sideTotal = ref(0)
const sideTitle = ref('本专题文章')
const form = ref({ content: '', parentId: null, captchaId: '', captchaCode: '' })
const captchaRef = ref(null)
const replyTo = ref(null)
const msg = ref('')
const detailMain = ref(null)
const sideRail = ref(null)
const favorited = ref(false)
const favoriteBusy = ref(false)
const SIDE_SIZE = 500

const contextQuery = computed(() => {
  const q = {}
  if (route.query.tagId) q.tagId = String(route.query.tagId)
  if (route.query.categoryId) q.categoryId = String(route.query.categoryId)
  return q
})

const series = computed(() => article.value?.series || null)

const commentTree = computed(() => {
  const list = comments.value || []
  const byId = new Map(list.map((c) => [c.id, { ...c, children: [] }]))
  const roots = []
  byId.forEach((c) => {
    if (c.parentId && byId.has(c.parentId)) {
      byId.get(c.parentId).children.push(c)
    } else {
      roots.push(c)
    }
  })
  return roots
})

async function load() {
  const res = await fetchArticle(route.params.slug)
  article.value = res.data
  document.title = `${article.value.seoTitle || article.value.title} - 技术实践笔记`
  const meta = document.querySelector('meta[name="description"]') || Object.assign(document.createElement('meta'), { name: 'description' })
  meta.content = article.value.seoDescription || article.value.summary || ''
  if (!meta.parentNode) document.head.appendChild(meta)
  await Promise.all([loadSideList(), loadCategoryToc(), loadFavoriteState()])
  await nextTick()
  if (detailMain.value) detailMain.value.scrollTop = 0
  progress.value = 0
  buildToc()
  enhanceExternalLinks()
  scrollActiveSideItem()
  markProgress()
  const c = await fetchComments({ articleId: article.value.id })
  comments.value = c.data.records || []
  form.value = { content: '', parentId: null, captchaId: '', captchaCode: '' }
  replyTo.value = null
  msg.value = ''
}

async function loadCategoryToc() {
  categoryToc.value = []
  const key = article.value?.categorySlug || article.value?.categoryId
  if (!key) return
  try {
    const res = await fetchCategoryToc(key)
    categoryToc.value = res.data?.items || res.data?.records || res.data || []
  } catch {
    categoryToc.value = []
  }
}

async function loadFavoriteState() {
  favorited.value = Boolean(article.value?.favorited)
  if (!auth.isLoggedIn || !article.value?.id) return
  try {
    const res = await fetchFavorites()
    const list = res.data?.records || res.data || []
    favorited.value = list.some((a) => (a.articleId || a.id) === article.value.id)
  } catch {
    /* keep article.favorited if present */
  }
}

async function markProgress() {
  if (!auth.isLoggedIn || !article.value?.id || !article.value?.categoryId) return
  try {
    await saveReadingProgress({
      categoryId: article.value.categoryId,
      articleId: article.value.id,
    })
  } catch {
    /* ignore */
  }
}

async function toggleFavorite() {
  if (!auth.isLoggedIn) {
    router.push({ path: '/login', query: { redirect: route.fullPath } })
    return
  }
  if (!article.value?.id || favoriteBusy.value) return
  favoriteBusy.value = true
  try {
    if (favorited.value) {
      await removeFavorite(article.value.id)
      favorited.value = false
    } else {
      await addFavorite(article.value.id)
      favorited.value = true
    }
  } catch (e) {
    msg.value = e.message || '操作失败'
  } finally {
    favoriteBusy.value = false
  }
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

function startReply(c) {
  replyTo.value = c
  form.value.parentId = c.id
}

function cancelReply() {
  replyTo.value = null
  form.value.parentId = null
}

async function submit() {
  msg.value = ''
  if (!auth.isLoggedIn) {
    router.push({ path: '/login', query: { redirect: route.fullPath } })
    return
  }
  try {
    const payload = {
      articleId: article.value.id,
      content: form.value.content,
      captchaId: form.value.captchaId,
      captchaCode: form.value.captchaCode,
    }
    if (form.value.parentId) payload.parentId = form.value.parentId
    await postComment(payload)
    msg.value = '评论已提交，审核通过后显示。'
    form.value.content = ''
    form.value.captchaCode = ''
    cancelReply()
    captchaRef.value?.refresh?.()
  } catch (e) {
    msg.value = e.message || '提交失败'
    captchaRef.value?.refresh?.()
  }
}

function seriesLink(item) {
  if (!item?.slug) return null
  return { name: 'article', params: { slug: item.slug }, query: contextQuery.value }
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

const displaySideList = computed(() => {
  if (categoryToc.value.length && !route.query.tagId) {
    return categoryToc.value.map((item) => ({
      id: item.id || item.articleId,
      slug: item.slug || item.articleSlug,
      title: item.title || item.articleTitle,
    }))
  }
  return sideList.value
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
              <strong>{{ categoryToc.length && !route.query.tagId ? '专题目录' : sideTitle }}</strong>
              <span class="muted" v-if="categoryToc.length && !route.query.tagId">{{ categoryToc.length }}</span>
              <span class="muted" v-else-if="sideTotal">{{ Math.min(sideList.length, sideTotal) }}/{{ sideTotal }}</span>
            </div>
            <nav class="side-rail-list">
              <RouterLink
                v-for="item in displaySideList"
                :key="item.id || item.slug"
                :to="{ name: 'article', params: { slug: item.slug }, query: contextQuery }"
                :class="{ 'is-active': item.slug === article.slug }"
                :title="item.title"
              >
                {{ item.title }}
              </RouterLink>
            </nav>
            <RouterLink
              v-if="!categoryToc.length && sideTotal > sideList.length"
              class="side-rail-more"
              :to="moreLink"
            >
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
            <button
              type="button"
              class="fav-btn"
              :class="{ on: favorited }"
              :disabled="favoriteBusy"
              @click="toggleFavorite"
            >
              {{ favorited ? '已收藏' : '收藏' }}
            </button>
          </div>
          <h1 class="hero-brand article-title">{{ article.title }}</h1>
          <p class="muted">{{ article.summary }}</p>

          <div v-if="series" class="series-nav panel">
            <div class="series-progress">
              系列进度 {{ series.index }}/{{ series.total }}
            </div>
            <div class="series-links">
              <RouterLink v-if="seriesLink(series.prev)" class="back-link" :to="seriesLink(series.prev)">
                ← {{ series.prev.title || '上一篇' }}
              </RouterLink>
              <span v-else class="muted">← 上一篇</span>
              <RouterLink v-if="seriesLink(series.next)" class="back-link" :to="seriesLink(series.next)">
                {{ series.next.title || '下一篇' }} →
              </RouterLink>
              <span v-else class="muted">下一篇 →</span>
            </div>
          </div>

          <article class="article-body" v-html="article.contentHtml"></article>

          <div v-if="series" class="series-nav panel" style="margin-top: 1.5rem">
            <div class="series-links">
              <RouterLink v-if="seriesLink(series.prev)" class="back-link" :to="seriesLink(series.prev)">
                ← 上一篇
              </RouterLink>
              <span v-else class="muted">← 上一篇</span>
              <span class="muted">{{ series.index }}/{{ series.total }}</span>
              <RouterLink v-if="seriesLink(series.next)" class="back-link" :to="seriesLink(series.next)">
                下一篇 →
              </RouterLink>
              <span v-else class="muted">下一篇 →</span>
            </div>
          </div>

          <div class="comment-block">
            <h2 style="font-family: var(--font-serif)">评论</h2>
            <div v-if="!comments.length" class="muted">暂无评论，来做第一个读者吧。</div>

            <template v-for="c in commentTree" :key="c.id">
              <div class="panel comment-item" style="margin-top: 0.75rem">
                <div class="meta comment-meta">
                  <img v-if="c.avatar" class="comment-avatar" :src="c.avatar" alt="" />
                  <strong>{{ c.nickname }}</strong>
                  <span>{{ String(c.createdAt || '').replace('T', ' ').slice(0, 16) }}</span>
                </div>
                <p style="margin: 0.4rem 0 0">{{ c.content }}</p>
                <button type="button" class="reply-btn" @click="startReply(c)">回复</button>
              </div>
              <div v-for="child in c.children" :key="child.id" class="panel comment-item comment-reply">
                <div class="meta comment-meta">
                  <img v-if="child.avatar" class="comment-avatar" :src="child.avatar" alt="" />
                  <strong>{{ child.nickname }}</strong>
                  <span class="reply-parent">回复 @{{ c.nickname }}</span>
                  <span>{{ String(child.createdAt || '').replace('T', ' ').slice(0, 16) }}</span>
                </div>
                <p style="margin: 0.4rem 0 0">{{ child.content }}</p>
                <button type="button" class="reply-btn" @click="startReply(child)">回复</button>
              </div>
            </template>

            <div v-if="!auth.isLoggedIn" class="comment-login-tip panel" style="margin-top: 1rem">
              <p style="margin: 0 0 0.75rem">登录后即可发表评论，昵称与头像将使用你的账号资料。</p>
              <RouterLink
                class="btn"
                :to="{ path: '/login', query: { redirect: route.fullPath } }"
              >
                去登录
              </RouterLink>
            </div>
            <form v-else class="comment-form" @submit.prevent="submit">
              <div class="comment-user">
                <img v-if="auth.member?.avatar" class="comment-avatar" :src="auth.member.avatar" alt="" />
                <strong>{{ auth.member?.nickname }}</strong>
              </div>
              <div v-if="replyTo" class="reply-tip">
                回复 @{{ replyTo.nickname }}
                <button type="button" class="reply-btn" @click="cancelReply">取消</button>
              </div>
              <textarea v-model="form.content" required rows="4" placeholder="友善发言，审核后展示" maxlength="1000"></textarea>
              <CaptchaField ref="captchaRef" v-model="form" />
              <div>
                <button class="btn" type="submit">{{ replyTo ? '提交回复' : '提交评论' }}</button>
                <span class="muted" style="margin-left: 0.75rem">{{ msg }}</span>
              </div>
            </form>
          </div>
        </div>

        <aside class="toc-rail">
          <div class="toc" v-if="toc.length">
            <strong>本文目录</strong>
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
          <div class="toc category-toc" v-if="categoryToc.length" style="margin-top: 1rem">
            <strong>专题目录</strong>
            <RouterLink
              v-for="item in categoryToc"
              :key="item.id || item.slug"
              :to="{ name: 'article', params: { slug: item.slug || item.articleSlug }, query: contextQuery }"
              :class="{ 'is-active': (item.slug || item.articleSlug) === article.slug }"
            >
              {{ item.title || item.articleTitle }}
            </RouterLink>
          </div>
        </aside>
      </div>
    </section>
  </div>
</template>
