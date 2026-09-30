import { createRouter, createWebHistory } from 'vue-router'
import HomeView from '../views/HomeView.vue'
import ArticlesView from '../views/ArticlesView.vue'
import ArticleDetailView from '../views/ArticleDetailView.vue'
import CategoriesView from '../views/CategoriesView.vue'
import CategoryDetailView from '../views/CategoryDetailView.vue'
import ProjectsView from '../views/ProjectsView.vue'
import AboutView from '../views/AboutView.vue'
import SearchView from '../views/SearchView.vue'
import AuthView from '../views/AuthView.vue'
import MemberView from '../views/MemberView.vue'
import { useAuthStore } from '../stores/auth'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', name: 'home', component: HomeView },
    { path: '/articles', name: 'articles', component: ArticlesView },
    { path: '/articles/:slug', name: 'article', component: ArticleDetailView },
    { path: '/categories', name: 'categories', component: CategoriesView },
    { path: '/categories/:slug', name: 'category', component: CategoryDetailView },
    { path: '/projects', name: 'projects', component: ProjectsView },
    { path: '/about', name: 'about', component: AboutView },
    { path: '/search', name: 'search', component: SearchView },
    { path: '/login', name: 'login', component: AuthView, props: { mode: 'login' } },
    { path: '/register', name: 'register', component: AuthView, props: { mode: 'register' } },
    { path: '/me', name: 'me', component: MemberView, meta: { requiresAuth: true } },
  ],
  scrollBehavior() {
    return { top: 0 }
  },
})

router.beforeEach((to) => {
  if (!to.meta.requiresAuth) return true
  const auth = useAuthStore()
  if (auth.isLoggedIn) return true
  return { path: '/login', query: { redirect: to.fullPath } }
})

export default router
