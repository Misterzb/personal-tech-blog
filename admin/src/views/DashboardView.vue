<script setup>
import { onMounted, ref } from 'vue'
import { dashboard } from '../api'

const data = ref({
  articleCount: 0,
  publishedCount: 0,
  draftCount: 0,
  totalPv: 0,
  pendingComments: 0,
  projectCount: 0,
  recentArticles: [],
  dailyStats: [],
})

onMounted(async () => {
  const res = await dashboard({ days: 30 })
  data.value = res.data
})
</script>

<template>
  <div>
    <h2>仪表盘</h2>
    <el-row :gutter="16">
      <el-col :span="4"><el-statistic title="文章总数" :value="data.articleCount" /></el-col>
      <el-col :span="4"><el-statistic title="已发布" :value="data.publishedCount" /></el-col>
      <el-col :span="4"><el-statistic title="草稿" :value="data.draftCount" /></el-col>
      <el-col :span="4"><el-statistic title="总 PV" :value="data.totalPv" /></el-col>
      <el-col :span="4"><el-statistic title="待审评论" :value="data.pendingComments" /></el-col>
      <el-col :span="4"><el-statistic title="开源项目" :value="data.projectCount" /></el-col>
    </el-row>

    <el-card style="margin-top: 20px" header="近 30 日 PV">
      <el-table :data="data.dailyStats" size="small">
        <el-table-column prop="statDate" label="日期" />
        <el-table-column prop="pv" label="PV" />
      </el-table>
    </el-card>

    <el-card style="margin-top: 20px" header="最近更新文章">
      <el-table :data="data.recentArticles" size="small">
        <el-table-column prop="title" label="标题" />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">{{ row.status === 1 ? '已发布' : '草稿' }}</template>
        </el-table-column>
        <el-table-column prop="updatedAt" label="更新时间" width="180" />
      </el-table>
    </el-card>
  </div>
</template>
