<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { deleteTag, fetchTags, saveTag } from '../api'

const list = ref([])
const dialog = ref(false)
const form = reactive({ id: null, name: '', slug: '' })

async function load() {
  list.value = (await fetchTags()).data
}
function open(row) {
  Object.assign(form, row || { id: null, name: '', slug: '' })
  dialog.value = true
}
async function submit() {
  await saveTag(form)
  ElMessage.success('已保存')
  dialog.value = false
  load()
}
async function remove(row) {
  await ElMessageBox.confirm(`删除标签「${row.name}」？`, '提示')
  await deleteTag(row.id)
  load()
}
onMounted(load)
</script>

<template>
  <div>
    <div style="display:flex;justify-content:space-between;margin-bottom:16px">
      <h2 style="margin:0">标签管理</h2>
      <el-button type="primary" @click="open()">新增标签</el-button>
    </div>
    <el-table :data="list">
      <el-table-column prop="name" label="名称" />
      <el-table-column prop="slug" label="Slug" />
      <el-table-column label="操作" width="160">
        <template #default="{ row }">
          <el-button link type="primary" @click="open(row)">编辑</el-button>
          <el-button link type="danger" @click="remove(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <el-dialog v-model="dialog" title="标签" width="420px">
      <el-form label-width="70px">
        <el-form-item label="名称"><el-input v-model="form.name" /></el-form-item>
        <el-form-item label="Slug"><el-input v-model="form.slug" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialog=false">取消</el-button>
        <el-button type="primary" @click="submit">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>
