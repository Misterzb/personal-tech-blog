import http from './http'

export const fetchHome = () => http.get('/api/public/home')
export const fetchSite = () => http.get('/api/public/site')
export const fetchArticles = (params) => http.get('/api/public/articles', { params })
export const fetchArticle = (slug) => http.get(`/api/public/articles/${slug}`)
export const searchArticles = (params) => http.get('/api/public/search', { params })
export const fetchCategories = () => http.get('/api/public/categories')
export const fetchCategory = (slug) => http.get(`/api/public/categories/${slug}`)
export const fetchTags = () => http.get('/api/public/tags')
export const fetchProjects = () => http.get('/api/public/projects')
export const fetchComments = (params) => http.get('/api/public/comments', { params })
export const postComment = (data) => http.post('/api/public/comments', data)
export const trackVisit = (path) => http.post('/api/public/visit', { path })
