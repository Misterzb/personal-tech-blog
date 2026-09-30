<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { fetchMembers, updateMemberStatus } from '../api'

const list = ref([])
const total = ref(0)
const page = ref(1)
const kw = ref('')
const status = ref('')

async function load() {
  const params = { page: page.value, size: 20 }
  if (kw.value.trim()) params.kw = kw.value.trim()
  if (status.value !== '' && status.value != null) params.status = status.value
  const res = await fetchMembers(params)
  list.value = res.data?.records || []
  total.value = res.data?.total || 0
}

async function toggleStatus(row) {
  const next = row.status === 1 ? 0 : 1
  const tip = next === 1 ? '启用' : '禁用'
  await ElMessageBox.confirm(`确认${tip}会员「${row.nickname || row.phone}」？`, '提示')
  await updateMemberStatus(row.id, next)
  ElMessage.success('已更新')
  load()
}

onMounted(load)
</script>

<template>
  <div>
    <div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:16px;gap:12px;flex-wrap:wrap">
      <h2 style="margin:0">会员管理</h2>
      <div style="display:flex;gap:8px;flex-wrap:wrap">
        <el-input v-model="kw" clearable placeholder="手机号/昵称" style="width:200px" @keyup.enter="page=1;load()" />
        <el-select v-model="status" clearable placeholder="状态" style="width:120px" @change="page=1;load()">
          <el-option label="正常" :value="1" />
          <el-option label="禁用" :value="0" />
        </el-select>
        <el-button type="primary" @click="page=1;load()">查询</el-button>
      </div>
    </div>
    <el-table :data="list">
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="phone" label="手机号" width="130" />
      <el-table-column prop="nickname" label="昵称" width="140" />
      <el-table-column prop="email" label="邮箱" min-width="160" />
      <el-table-column label="头像" width="80">
        <template #default="{ row }">
          <el-avatar v-if="row.avatar" :src="row.avatar" :size="32" />
          <span v-else>—</span>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="90">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small">
            {{ row.status === 1 ? '正常' : '禁用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createdAt" label="注册时间" width="180" />
      <el-table-column label="操作" width="100" fixed="right">
        <template #default="{ row }">
          <el-button
            link
            :type="row.status === 1 ? 'danger' : 'success'"
            @click="toggleStatus(row)"
          >
            {{ row.status === 1 ? '禁用' : '启用' }}
          </el-button>
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
