<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { deleteComment, fetchComments, updateCommentStatus } from '../api'

const list = ref([])
const total = ref(0)
const page = ref(1)
const status = ref(0)

async function load() {
  const res = await fetchComments({ page: page.value, size: 20, status: status.value })
  list.value = res.data.records
  total.value = res.data.total
}

async function approve(row, s) {
  await updateCommentStatus(row.id, s)
  ElMessage.success('已更新')
  load()
}

async function remove(row) {
  await deleteComment(row.id)
  load()
}

onMounted(load)
</script>

<template>
  <div>
    <div style="display:flex;justify-content:space-between;margin-bottom:16px">
      <h2 style="margin:0">评论审核</h2>
      <el-select v-model="status" style="width:160px" @change="page=1;load()">
        <el-option label="待审核" :value="0" />
        <el-option label="已通过" :value="1" />
        <el-option label="已拒绝" :value="2" />
      </el-select>
    </div>
    <el-table :data="list">
      <el-table-column prop="nickname" label="昵称" width="120" />
      <el-table-column prop="articleId" label="文章ID" width="90" />
      <el-table-column prop="content" label="内容" />
      <el-table-column prop="createdAt" label="时间" width="180" />
      <el-table-column label="操作" width="220">
        <template #default="{ row }">
          <el-button link type="success" @click="approve(row, 1)">通过</el-button>
          <el-button link type="warning" @click="approve(row, 2)">拒绝</el-button>
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
