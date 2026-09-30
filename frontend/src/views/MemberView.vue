<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  fetchFavorites,
  fetchMyComments,
  fetchNotifications,
  fetchSubscriptions,
  markAllNotificationsRead,
  markNotificationRead,
  removeFavorite,
  removeSubscription,
  updatePassword,
  updateProfile,
  uploadAvatar,
} from '../api'
import { useAuthStore } from '../stores/auth'

const auth = useAuthStore()
const route = useRoute()
const router = useRouter()

const tabs = [
  { key: 'profile', label: '资料' },
  { key: 'password', label: '修改密码' },
  { key: 'comments', label: '我的评论' },
  { key: 'favorites', label: '收藏' },
  { key: 'subscriptions', label: '订阅' },
  { key: 'notifications', label: '通知' },
]

const tab = ref('profile')
const msg = ref('')
const err = ref('')
const loading = ref(false)

const profile = reactive({ nickname: '', email: '' })
const pwd = reactive({ oldPassword: '', newPassword: '', confirm: '' })

const comments = ref([])
const commentsTotal = ref(0)
const commentsPage = ref(1)

const favorites = ref([])
const subscriptions = ref([])

const notifications = ref([])
const notificationsTotal = ref(0)
const notificationsPage = ref(1)

const validTab = computed(() => tabs.some((t) => t.key === tab.value))

function syncTabFromRoute() {
  const q = typeof route.query.tab === 'string' ? route.query.tab : 'profile'
  tab.value = tabs.some((t) => t.key === q) ? q : 'profile'
}

function switchTab(key) {
  tab.value = key
  router.replace({ path: '/me', query: key === 'profile' ? {} : { tab: key } })
  msg.value = ''
  err.value = ''
  loadTabData()
}

function fillProfile() {
  profile.nickname = auth.member?.nickname || ''
  profile.email = auth.member?.email || ''
}

async function saveProfile() {
  msg.value = ''
  err.value = ''
  loading.value = true
  try {
    await updateProfile({ nickname: profile.nickname, email: profile.email })
    await auth.refreshMe()
    fillProfile()
    msg.value = '资料已保存'
  } catch (e) {
    err.value = e.message || '保存失败'
  } finally {
    loading.value = false
  }
}

async function onAvatarChange(e) {
  const file = e.target.files?.[0]
  if (!file) return
  msg.value = ''
  err.value = ''
  loading.value = true
  try {
    await uploadAvatar(file)
    await auth.refreshMe()
    msg.value = '头像已更新'
  } catch (ex) {
    err.value = ex.message || '上传失败'
  } finally {
    loading.value = false
    e.target.value = ''
  }
}

async function savePassword() {
  msg.value = ''
  err.value = ''
  if (pwd.newPassword !== pwd.confirm) {
    err.value = '两次输入的新密码不一致'
    return
  }
  loading.value = true
  try {
    await updatePassword({ oldPassword: pwd.oldPassword, newPassword: pwd.newPassword })
    pwd.oldPassword = ''
    pwd.newPassword = ''
    pwd.confirm = ''
    msg.value = '密码已修改'
  } catch (e) {
    err.value = e.message || '修改失败'
  } finally {
    loading.value = false
  }
}

async function loadComments() {
  const res = await fetchMyComments({ page: commentsPage.value, size: 10 })
  comments.value = res.data?.records || res.data || []
  commentsTotal.value = res.data?.total || comments.value.length
}

async function loadFavorites() {
  const res = await fetchFavorites()
  favorites.value = res.data?.records || res.data || []
}

async function loadSubscriptions() {
  const res = await fetchSubscriptions()
  subscriptions.value = res.data?.records || res.data || []
}

async function loadNotifications() {
  const res = await fetchNotifications({ page: notificationsPage.value, size: 10 })
  notifications.value = res.data?.records || res.data || []
  notificationsTotal.value = res.data?.total || notifications.value.length
}

async function unfavorite(id) {
  await removeFavorite(id)
  await loadFavorites()
}

async function unsubscribe(id) {
  await removeSubscription(id)
  await loadSubscriptions()
}

async function readOne(id) {
  await markNotificationRead(id)
  await loadNotifications()
}

async function readAll() {
  await markAllNotificationsRead()
  await loadNotifications()
}

async function loadTabData() {
  try {
    if (tab.value === 'comments') await loadComments()
    if (tab.value === 'favorites') await loadFavorites()
    if (tab.value === 'subscriptions') await loadSubscriptions()
    if (tab.value === 'notifications') await loadNotifications()
  } catch (e) {
    err.value = e.message || '加载失败'
  }
}

function formatTime(v) {
  return v ? String(v).replace('T', ' ').slice(0, 16) : ''
}

onMounted(async () => {
  document.title = '会员中心 - 技术实践笔记'
  if (!auth.member) await auth.refreshMe()
  fillProfile()
  syncTabFromRoute()
  await loadTabData()
})

watch(() => route.query.tab, () => {
  syncTabFromRoute()
  loadTabData()
})
</script>

<template>
  <section class="section container me-page">
    <div class="section-head">
      <div>
        <h2>会员中心</h2>
        <p class="muted">管理资料、收藏、订阅与通知。</p>
      </div>
    </div>

    <div class="me-layout">
      <nav class="me-tabs panel">
        <button
          v-for="t in tabs"
          :key="t.key"
          type="button"
          :class="{ active: tab === t.key }"
          @click="switchTab(t.key)"
        >
          {{ t.label }}
        </button>
      </nav>

      <div class="me-panel panel" v-if="validTab">
        <p v-if="msg" class="me-msg ok">{{ msg }}</p>
        <p v-if="err" class="me-msg err">{{ err }}</p>

        <div v-if="tab === 'profile'" class="me-form">
          <div class="avatar-row">
            <img v-if="auth.member?.avatar" class="me-avatar" :src="auth.member.avatar" alt="" />
            <div v-else class="me-avatar placeholder">{{ (profile.nickname || '?').slice(0, 1) }}</div>
            <label class="btn ghost avatar-btn">
              更换头像
              <input type="file" accept="image/*" hidden @change="onAvatarChange" />
            </label>
          </div>
          <label>
            手机号
            <input :value="auth.member?.phone" disabled />
          </label>
          <label>
            昵称
            <input v-model="profile.nickname" maxlength="32" required />
          </label>
          <label>
            邮箱
            <input v-model="profile.email" type="email" maxlength="128" />
          </label>
          <button class="btn" type="button" :disabled="loading" @click="saveProfile">保存资料</button>
        </div>

        <form v-else-if="tab === 'password'" class="me-form" @submit.prevent="savePassword">
          <label>
            原密码
            <input v-model="pwd.oldPassword" type="password" required minlength="6" />
          </label>
          <label>
            新密码
            <input v-model="pwd.newPassword" type="password" required minlength="6" />
          </label>
          <label>
            确认新密码
            <input v-model="pwd.confirm" type="password" required minlength="6" />
          </label>
          <button class="btn" type="submit" :disabled="loading">更新密码</button>
        </form>

        <div v-else-if="tab === 'comments'">
          <div v-if="!comments.length" class="muted">暂无评论。</div>
          <div v-for="c in comments" :key="c.id" class="me-item">
            <div class="meta">
              <RouterLink v-if="c.articleSlug" :to="`/articles/${c.articleSlug}`">{{ c.articleTitle || '文章' }}</RouterLink>
              <span v-else>{{ c.articleTitle || `文章 #${c.articleId}` }}</span>
              <span>{{ formatTime(c.createdAt) }}</span>
              <span v-if="c.status === 0">待审</span>
              <span v-else-if="c.status === 2">已拒绝</span>
            </div>
            <p>{{ c.content }}</p>
          </div>
          <div class="pager" v-if="commentsTotal > 10">
            <button class="btn ghost" type="button" :disabled="commentsPage <= 1" @click="commentsPage--; loadComments()">上一页</button>
            <button class="btn ghost" type="button" :disabled="commentsPage * 10 >= commentsTotal" @click="commentsPage++; loadComments()">下一页</button>
          </div>
        </div>

        <div v-else-if="tab === 'favorites'">
          <div v-if="!favorites.length" class="muted">暂无收藏。</div>
          <div v-for="a in favorites" :key="a.id || a.articleId" class="me-item me-row">
            <div>
              <RouterLink :to="`/articles/${a.slug || a.articleSlug}`">{{ a.title || a.articleTitle }}</RouterLink>
              <p class="muted" style="margin: 0.35rem 0 0">{{ a.summary }}</p>
            </div>
            <button class="btn ghost" type="button" @click="unfavorite(a.articleId || a.id)">取消收藏</button>
          </div>
        </div>

        <div v-else-if="tab === 'subscriptions'">
          <div v-if="!subscriptions.length" class="muted">暂无订阅专题。</div>
          <div v-for="c in subscriptions" :key="c.id || c.categoryId" class="me-item me-row">
            <div>
              <RouterLink :to="`/categories/${c.slug || c.categorySlug}`">{{ c.name || c.categoryName }}</RouterLink>
              <p class="muted" style="margin: 0.35rem 0 0">{{ c.description }}</p>
            </div>
            <button class="btn ghost" type="button" @click="unsubscribe(c.categoryId || c.id)">取消订阅</button>
          </div>
        </div>

        <div v-else-if="tab === 'notifications'">
          <div class="me-row" style="margin-bottom: 1rem">
            <span class="muted">共 {{ notificationsTotal }} 条</span>
            <button class="btn ghost" type="button" @click="readAll">全部已读</button>
          </div>
          <div v-if="!notifications.length" class="muted">暂无通知。</div>
          <div
            v-for="n in notifications"
            :key="n.id"
            class="me-item"
            :class="{ unread: !n.read && n.read !== 1 && n.isRead !== 1 }"
          >
            <div class="meta">
              <strong>{{ n.title || n.type || '通知' }}</strong>
              <span>{{ formatTime(n.createdAt) }}</span>
            </div>
            <p>{{ n.content || n.message }}</p>
            <button
              v-if="!n.read && n.read !== 1 && n.isRead !== 1"
              class="btn ghost"
              type="button"
              style="margin-top: 0.5rem"
              @click="readOne(n.id)"
            >
              标为已读
            </button>
          </div>
          <div class="pager" v-if="notificationsTotal > 10">
            <button class="btn ghost" type="button" :disabled="notificationsPage <= 1" @click="notificationsPage--; loadNotifications()">上一页</button>
            <button class="btn ghost" type="button" :disabled="notificationsPage * 10 >= notificationsTotal" @click="notificationsPage++; loadNotifications()">下一页</button>
          </div>
        </div>
      </div>
    </div>
  </section>
</template>

<style scoped>
.me-layout {
  display: grid;
  grid-template-columns: 200px minmax(0, 1fr);
  gap: 1.25rem;
  align-items: start;
}
.me-tabs {
  display: flex;
  flex-direction: column;
  gap: 0.35rem;
  padding: 0.85rem;
}
.me-tabs button {
  border: none;
  background: transparent;
  text-align: left;
  padding: 0.65rem 0.85rem;
  border-radius: 10px;
  color: var(--muted);
  cursor: pointer;
  font: inherit;
}
.me-tabs button.active,
.me-tabs button:hover {
  background: var(--accent-soft);
  color: var(--accent);
  font-weight: 600;
}
.me-panel {
  min-height: 280px;
}
.me-form {
  display: grid;
  gap: 0.9rem;
  max-width: 420px;
}
.me-form label {
  display: grid;
  gap: 0.35rem;
  font-size: 0.92rem;
  color: var(--muted);
}
.me-form input {
  border: 1px solid var(--line);
  border-radius: 10px;
  padding: 0.65rem 0.75rem;
  font: inherit;
  color: var(--ink);
  background: var(--input-bg);
}
.me-form input:disabled {
  opacity: 0.7;
}
.avatar-row {
  display: flex;
  align-items: center;
  gap: 1rem;
}
.me-avatar {
  width: 64px;
  height: 64px;
  border-radius: 50%;
  object-fit: cover;
  background: var(--accent-soft);
}
.me-avatar.placeholder {
  display: grid;
  place-items: center;
  font-family: var(--font-serif);
  font-size: 1.4rem;
  color: var(--accent);
}
.avatar-btn {
  cursor: pointer;
}
.me-item {
  padding: 0.9rem 0;
  border-bottom: 1px solid var(--line);
}
.me-item:last-child {
  border-bottom: none;
}
.me-item p {
  margin: 0.4rem 0 0;
}
.me-item.unread {
  background: var(--accent-soft);
  margin: 0 -0.75rem;
  padding-left: 0.75rem;
  padding-right: 0.75rem;
  border-radius: 10px;
  border-bottom-color: transparent;
}
.me-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 1rem;
}
.me-msg {
  margin: 0 0 1rem;
  font-size: 0.92rem;
}
.me-msg.ok { color: var(--accent); }
.me-msg.err { color: #b91c1c; }
@media (max-width: 720px) {
  .me-layout {
    grid-template-columns: 1fr;
  }
  .me-tabs {
    flex-direction: row;
    flex-wrap: wrap;
  }
}
</style>
