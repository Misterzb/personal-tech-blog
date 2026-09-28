<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { MdEditor } from 'md-editor-v3'
import 'md-editor-v3/lib/style.css'
import { fetchArticle, fetchCategories, fetchTags, saveArticle, uploadFile } from '../api'

const route = useRoute()
const router = useRouter()
const categories = ref([])
const tags = ref([])
const form = reactive({
  id: null,
  title: '',
  slug: '',
  summary: '',
  contentMd: '',
  cover: '',
  status: 0,
  categoryId: null,
  seoTitle: '',
  seoDescription: '',
  isTop: false,
  tagIds: [],
})

async function load() {
  const [c, t] = await Promise.all([fetchCategories(), fetchTags()])
  categories.value = c.data
  tags.value = t.data
  if (route.params.id) {
    const res = await fetchArticle(route.params.id)
    Object.assign(form, {
      ...res.data,
      tagIds: (res.data.tags || []).map((x) => x.id),
      isTop: !!res.data.isTop,
    })
  }
}

async function onUpload(files, callback) {
  const urls = []
  for (const file of files) {
    const res = await uploadFile(file)
    urls.push(res.data.url)
  }
  callback(urls)
}

async function submit(status) {
  form.status = status
  await saveArticle(form)
  ElMessage.success(status === 1 ? '已发布' : '已保存草稿')
  router.push('/articles')
}

onMounted(load)
</script>

<template>
  <div>
    <div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:16px">
      <h2 style="margin:0">{{ form.id ? '编辑文章' : '写文章' }}</h2>
      <div>
        <el-button @click="submit(0)">保存草稿</el-button>
        <el-button type="primary" @click="submit(1)">发布</el-button>
      </div>
    </div>
    <el-form label-width="90px">
      <el-form-item label="标题"><el-input v-model="form.title" /></el-form-item>
      <el-form-item label="Slug"><el-input v-model="form.slug" placeholder="可空，自动生成" /></el-form-item>
      <el-form-item label="摘要"><el-input v-model="form.summary" type="textarea" :rows="2" /></el-form-item>
      <el-form-item label="专题">
        <el-select v-model="form.categoryId" clearable style="width: 240px">
          <el-option v-for="c in categories" :key="c.id" :label="c.name" :value="c.id" />
        </el-select>
      </el-form-item>
      <el-form-item label="标签">
        <el-select v-model="form.tagIds" multiple style="width: 420px">
          <el-option v-for="t in tags" :key="t.id" :label="t.name" :value="t.id" />
        </el-select>
      </el-form-item>
      <el-form-item label="封面 URL"><el-input v-model="form.cover" /></el-form-item>
      <el-form-item label="置顶"><el-switch v-model="form.isTop" /></el-form-item>
      <el-form-item label="SEO 标题"><el-input v-model="form.seoTitle" /></el-form-item>
      <el-form-item label="SEO 描述"><el-input v-model="form.seoDescription" /></el-form-item>
      <el-form-item label="正文">
        <MdEditor v-model="form.contentMd" language="zh-CN" style="width:100%" @onUploadImg="onUpload" />
      </el-form-item>
    </el-form>
  </div>
</template>
