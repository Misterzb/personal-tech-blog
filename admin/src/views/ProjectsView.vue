<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { deleteProject, fetchProjects, saveProject } from '../api'

const list = ref([])
const dialog = ref(false)
const empty = () => ({
  id: null,
  name: '',
  summary: '',
  techStack: '',
  githubUrl: '',
  giteeUrl: '',
  repoUrl: '',
  demoUrl: '',
  cover: '',
  sortOrder: 0,
  isTop: false,
})
const form = reactive(empty())

async function load() {
  list.value = (await fetchProjects()).data
}
function open(row) {
  Object.assign(form, empty(), row || {})
  // 旧数据只有 repoUrl 时回填到对应平台
  if (!form.githubUrl && form.repoUrl && String(form.repoUrl).includes('github.com')) {
    form.githubUrl = form.repoUrl
  }
  if (!form.giteeUrl && form.repoUrl && String(form.repoUrl).includes('gitee.com')) {
    form.giteeUrl = form.repoUrl
  }
  if (!form.githubUrl && !form.giteeUrl && form.repoUrl) {
    form.githubUrl = form.repoUrl
  }
  form.isTop = !!form.isTop
  dialog.value = true
}
async function submit() {
  await saveProject(form)
  ElMessage.success('已保存')
  dialog.value = false
  load()
}
async function remove(row) {
  await ElMessageBox.confirm(`删除项目「${row.name}」？`, '提示')
  await deleteProject(row.id)
  load()
}
onMounted(load)
</script>

<template>
  <div>
    <div style="display:flex;justify-content:space-between;margin-bottom:16px">
      <h2 style="margin:0">开源项目</h2>
      <el-button type="primary" @click="open()">新增项目</el-button>
    </div>
    <el-table :data="list">
      <el-table-column prop="name" label="名称" />
      <el-table-column prop="techStack" label="技术栈" />
      <el-table-column label="GitHub" min-width="160" show-overflow-tooltip>
        <template #default="{ row }">{{ row.githubUrl || '—' }}</template>
      </el-table-column>
      <el-table-column label="Gitee" min-width="160" show-overflow-tooltip>
        <template #default="{ row }">{{ row.giteeUrl || '—' }}</template>
      </el-table-column>
      <el-table-column prop="isTop" label="置顶" width="80">
        <template #default="{ row }">{{ row.isTop ? '是' : '否' }}</template>
      </el-table-column>
      <el-table-column label="操作" width="160">
        <template #default="{ row }">
          <el-button link type="primary" @click="open(row)">编辑</el-button>
          <el-button link type="danger" @click="remove(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <el-dialog v-model="dialog" title="项目" width="600px">
      <el-form label-width="100px">
        <el-form-item label="名称"><el-input v-model="form.name" /></el-form-item>
        <el-form-item label="简介"><el-input v-model="form.summary" type="textarea" /></el-form-item>
        <el-form-item label="技术栈"><el-input v-model="form.techStack" placeholder="逗号分隔" /></el-form-item>
        <el-form-item label="GitHub 地址">
          <el-input v-model="form.githubUrl" placeholder="https://github.com/xxx/yyy" />
        </el-form-item>
        <el-form-item label="Gitee 地址">
          <el-input v-model="form.giteeUrl" placeholder="https://gitee.com/xxx/yyy" />
        </el-form-item>
        <el-form-item label="演示地址"><el-input v-model="form.demoUrl" /></el-form-item>
        <el-form-item label="排序"><el-input-number v-model="form.sortOrder" /></el-form-item>
        <el-form-item label="置顶"><el-switch v-model="form.isTop" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialog=false">取消</el-button>
        <el-button type="primary" @click="submit">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>
