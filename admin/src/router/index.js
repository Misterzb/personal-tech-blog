import { createRouter, createWebHashHistory } from 'vue-router'
import LoginView from '../views/LoginView.vue'
import LayoutView from '../views/LayoutView.vue'
import DashboardView from '../views/DashboardView.vue'
import ArticlesView from '../views/ArticlesView.vue'
import ArticleEditView from '../views/ArticleEditView.vue'
import CategoriesView from '../views/CategoriesView.vue'
import TagsView from '../views/TagsView.vue'
import ProjectsView from '../views/ProjectsView.vue'
import CommentsView from '../views/CommentsView.vue'
import SiteView from '../views/SiteView.vue'

const router = createRouter({
  history: createWebHashHistory(),
  routes: [
    { path: '/login', component: LoginView },
    {
      path: '/',
      component: LayoutView,
      redirect: '/dashboard',
      children: [
        { path: 'dashboard', component: DashboardView },
        { path: 'articles', component: ArticlesView },
        { path: 'articles/edit/:id?', component: ArticleEditView },
        { path: 'categories', component: CategoriesView },
        { path: 'tags', component: TagsView },
        { path: 'projects', component: ProjectsView },
        { path: 'comments', component: CommentsView },
        { path: 'site', component: SiteView },
      ],
    },
  ],
})

router.beforeEach((to, _from, next) => {
  const token = localStorage.getItem('blog_token')
  if (to.path !== '/login' && !token) {
    next('/login')
  } else if (to.path === '/login' && token) {
    next('/dashboard')
  } else {
    next()
  }
})

export default router
