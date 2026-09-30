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
  memberCount: 0,
  memberGrowth7d: 0,
  memberGrowth30d: 0,
  recentArticles: [],
  dailyStats: [],
  hotArticles: [],
  categoryViews: [],
})

onMounted(async () => {
  const res = await dashboard({ days: 30 })
  data.value = { ...data.value, ...(res.data || {}) }
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
    <el-row :gutter="16" style="margin-top: 16px">
      <el-col :span="8"><el-statistic title="会员总数" :value="data.memberCount || 0" /></el-col>
      <el-col :span="8"><el-statistic title="近 7 日新增会员" :value="data.memberGrowth7d || 0" /></el-col>
      <el-col :span="8"><el-statistic title="近 30 日新增会员" :value="data.memberGrowth30d || 0" /></el-col>
    </el-row>

    <el-row :gutter="16" style="margin-top: 20px">
      <el-col :span="12">
        <el-card header="近 30 日 PV">
          <el-table :data="data.dailyStats" size="small" max-height="320">
            <el-table-column prop="statDate" label="日期" />
            <el-table-column prop="pv" label="PV" />
          </el-table>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card header="热门文章">
          <el-table :data="data.hotArticles || []" size="small" max-height="320">
            <el-table-column prop="title" label="标题" min-width="160" />
            <el-table-column prop="viewCount" label="阅读" width="90" />
          </el-table>
          <p v-if="!(data.hotArticles || []).length" style="color:#999;font-size:13px;margin:8px 0 0">暂无数据</p>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="16" style="margin-top: 20px">
      <el-col :span="12">
        <el-card header="专题浏览量">
          <el-table :data="data.categoryViews || []" size="small" max-height="280">
            <el-table-column prop="name" label="专题" min-width="140">
              <template #default="{ row }">{{ row.name || row.categoryName }}</template>
            </el-table-column>
            <el-table-column prop="viewCount" label="浏览" width="100">
              <template #default="{ row }">{{ row.viewCount ?? row.pv ?? 0 }}</template>
            </el-table-column>
          </el-table>
          <p v-if="!(data.categoryViews || []).length" style="color:#999;font-size:13px;margin:8px 0 0">暂无数据</p>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card header="最近更新文章">
          <el-table :data="data.recentArticles" size="small" max-height="280">
            <el-table-column prop="title" label="标题" />
            <el-table-column prop="status" label="状态" width="100">
              <template #default="{ row }">{{ row.status === 1 ? '已发布' : '草稿' }}</template>
            </el-table-column>
            <el-table-column prop="updatedAt" label="更新时间" width="180" />
          </el-table>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>
