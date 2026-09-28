<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { deleteCategory, fetchCategories, saveCategory } from '../api'

const list = ref([])
const dialog = ref(false)
const form = reactive({ id: null, name: '', slug: '', description: '', sortOrder: 0, cover: '' })

async function load() {
  const res = await fetchCategories()
  list.value = res.data
}

function open(row) {
  Object.assign(form, row || { id: null, name: '', slug: '', description: '', sortOrder: 0, cover: '' })
  dialog.value = true
}

async function submit() {
  await saveCategory(form)
  ElMessage.success('已保存')
  dialog.value = false
  load()
}

async function remove(row) {
  await ElMessageBox.confirm(`删除专题「${row.name}」？`, '提示')
  await deleteCategory(row.id)
  load()
}

onMounted(load)
</script>

<template>
  <div>
    <div style="display:flex;justify-content:space-between;margin-bottom:16px">
      <h2 style="margin:0">专题管理</h2>
      <el-button type="primary" @click="open()">新增专题</el-button>
    </div>
    <el-table :data="list">
      <el-table-column prop="name" label="名称" />
      <el-table-column prop="slug" label="Slug" />
      <el-table-column prop="description" label="简介" />
      <el-table-column prop="sortOrder" label="排序" width="80" />
      <el-table-column label="操作" width="160">
        <template #default="{ row }">
          <el-button link type="primary" @click="open(row)">编辑</el-button>
          <el-button link type="danger" @click="remove(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <el-dialog v-model="dialog" title="专题" width="520px">
      <el-form label-width="80px">
        <el-form-item label="名称"><el-input v-model="form.name" /></el-form-item>
        <el-form-item label="Slug"><el-input v-model="form.slug" /></el-form-item>
        <el-form-item label="简介"><el-input v-model="form.description" type="textarea" /></el-form-item>
        <el-form-item label="排序"><el-input-number v-model="form.sortOrder" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialog=false">取消</el-button>
        <el-button type="primary" @click="submit">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>
