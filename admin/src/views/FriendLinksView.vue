<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  deleteFriendLink,
  fetchFriendLinks,
  saveFriendLink,
  updateFriendLink,
} from '../api'

const list = ref([])
const total = ref(0)
const page = ref(1)
const dialog = ref(false)
const form = reactive({
  id: null,
  name: '',
  url: '',
  description: '',
  sortOrder: 0,
  status: 1,
})

async function load() {
  const res = await fetchFriendLinks({ page: page.value, size: 20 })
  list.value = res.data?.records || res.data || []
  total.value = res.data?.total || list.value.length
}

function open(row) {
  Object.assign(form, row || {
    id: null,
    name: '',
    url: '',
    description: '',
    sortOrder: 0,
    status: 1,
  })
  dialog.value = true
}

async function submit() {
  const payload = {
    name: form.name,
    url: form.url,
    description: form.description,
    sortOrder: form.sortOrder,
    status: form.status,
  }
  if (form.id) {
    await updateFriendLink(form.id, payload)
  } else {
    await saveFriendLink(payload)
  }
  ElMessage.success('已保存')
  dialog.value = false
  load()
}

async function remove(row) {
  await ElMessageBox.confirm(`删除友链「${row.name}」？`, '提示')
  await deleteFriendLink(row.id)
  ElMessage.success('已删除')
  load()
}

onMounted(load)
</script>

<template>
  <div>
    <div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:16px">
      <h2 style="margin:0">友情链接</h2>
      <el-button type="primary" @click="open()">新增友链</el-button>
    </div>
    <el-table :data="list">
      <el-table-column prop="name" label="名称" min-width="140" />
      <el-table-column prop="url" label="URL" min-width="220" show-overflow-tooltip />
      <el-table-column prop="description" label="简介" min-width="160" show-overflow-tooltip />
      <el-table-column prop="sortOrder" label="排序" width="80" />
      <el-table-column label="状态" width="90">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">
            {{ row.status === 1 ? '启用' : '停用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="140" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="open(row)">编辑</el-button>
          <el-button link type="danger" @click="remove(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <el-pagination
      v-if="total > 20"
      style="margin-top:16px;justify-content:flex-end"
      background
      layout="prev, pager, next, total"
      :total="total"
      v-model:current-page="page"
      @current-change="load"
    />
    <el-dialog v-model="dialog" :title="form.id ? '编辑友链' : '新增友链'" width="520px">
      <el-form label-width="80px">
        <el-form-item label="名称"><el-input v-model="form.name" /></el-form-item>
        <el-form-item label="URL"><el-input v-model="form.url" placeholder="https://" /></el-form-item>
        <el-form-item label="简介"><el-input v-model="form.description" type="textarea" /></el-form-item>
        <el-form-item label="排序"><el-input-number v-model="form.sortOrder" /></el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="form.status">
            <el-radio :value="1">启用</el-radio>
            <el-radio :value="0">停用</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialog=false">取消</el-button>
        <el-button type="primary" @click="submit">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>
