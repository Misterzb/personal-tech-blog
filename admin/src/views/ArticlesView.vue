<script setup>
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessageBox } from 'element-plus'
import { deleteArticle, fetchArticles } from '../api'

const router = useRouter()
const list = ref([])
const total = ref(0)
const page = ref(1)
const keyword = ref('')
const status = ref(null)

async function load() {
  const res = await fetchArticles({
    page: page.value,
    size: 10,
    keyword: keyword.value || undefined,
    status: status.value,
  })
  list.value = res.data.records
  total.value = res.data.total
}

async function remove(row) {
  await ElMessageBox.confirm(`确认删除「${row.title}」？`, '提示')
  await deleteArticle(row.id)
  load()
}

onMounted(load)
</script>

<template>
  <div>
    <div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:16px">
      <h2 style="margin:0">文章管理</h2>
      <el-button type="primary" @click="router.push('/articles/edit')">写文章</el-button>
    </div>
    <div style="display:flex;gap:12px;margin-bottom:12px">
      <el-input v-model="keyword" placeholder="搜索标题/摘要" clearable style="width:240px" @keyup.enter="page=1;load()" />
      <el-select v-model="status" clearable placeholder="状态" style="width:140px" @change="page=1;load()">
        <el-option label="草稿" :value="0" />
        <el-option label="已发布" :value="1" />
      </el-select>
      <el-button @click="page=1;load()">查询</el-button>
    </div>
    <el-table :data="list">
      <el-table-column prop="title" label="标题" min-width="150" />
      <el-table-column prop="categoryName" label="专题" min-width="140" />
      <el-table-column prop="status" label="状态" width="90">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'info'">{{ row.status === 1 ? '已发布' : '草稿' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="viewCount" label="阅读" width="90" />
      <el-table-column prop="updatedAt" label="更新时间" width="180" />
      <el-table-column label="操作" width="180">
        <template #default="{ row }">
          <el-button link type="primary" @click="router.push(`/articles/edit/${row.id}`)">编辑</el-button>
          <el-button link type="danger" @click="remove(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <el-pagination
      style="margin-top:16px;justify-content:flex-end"
      background
      layout="prev, pager, next, total"
      :total="total"
      v-model:current-page="page"
      @current-change="load"
    />
  </div>
</template>
