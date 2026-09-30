<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { deleteTag, fetchTaxonomyStats, saveTag } from '../api'

const list = ref([])
const emptyOnly = ref(false)
const dialog = ref(false)
const form = reactive({ id: null, name: '', slug: '' })

const displayList = computed(() => {
  if (!emptyOnly.value) return list.value
  return list.value.filter((x) => (x.publishedCount || 0) <= 0)
})

async function load() {
  const res = await fetchTaxonomyStats()
  list.value = res.data.tags || []
}

function open(row) {
  Object.assign(form, row || { id: null, name: '', slug: '' })
  dialog.value = true
}

async function submit() {
  await saveTag({ id: form.id, name: form.name, slug: form.slug })
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
    <div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:16px;gap:12px;flex-wrap:wrap">
      <div>
        <h2 style="margin:0">标签管理</h2>
        <p style="margin:6px 0 0;color:#888;font-size:13px">
          「已发布=0」的标签前台不展示。可勾选仅看空标签后决定是否删除。
        </p>
      </div>
      <div style="display:flex;gap:12px;align-items:center">
        <el-checkbox v-model="emptyOnly">仅看前台无数据</el-checkbox>
        <el-button type="primary" @click="open()">新增标签</el-button>
      </div>
    </div>
    <el-table :data="displayList">
      <el-table-column prop="name" label="名称" />
      <el-table-column prop="slug" label="Slug" />
      <el-table-column prop="publishedCount" label="已发布文章" width="110" />
      <el-table-column prop="draftCount" label="草稿文章" width="100" />
      <el-table-column prop="totalCount" label="文章合计" width="100" />
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
