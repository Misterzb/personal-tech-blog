<script setup>
import { onMounted, reactive } from 'vue'
import { ElMessage } from 'element-plus'
import { MdEditor } from 'md-editor-v3'
import 'md-editor-v3/lib/style.css'
import { changePassword, fetchSite, saveSite } from '../api'

const form = reactive({
  siteName: '',
  siteSubtitle: '',
  icp: '',
  aboutMd: '',
  socialLinks: '[]',
  logo: '',
})

const pwd = reactive({ oldPassword: '', newPassword: '' })

onMounted(async () => {
  const res = await fetchSite()
  Object.assign(form, res.data)
})

async function submit() {
  await saveSite(form)
  ElMessage.success('站点配置已保存')
}

async function submitPwd() {
  await changePassword(pwd)
  localStorage.removeItem('blog_must_change_password')
  ElMessage.success('密码已修改')
  pwd.oldPassword = ''
  pwd.newPassword = ''
}
</script>

<template>
  <div>
    <h2>站点配置</h2>
    <el-form label-width="100px" style="max-width: 900px">
      <el-form-item label="站点名称"><el-input v-model="form.siteName" /></el-form-item>
      <el-form-item label="副标题"><el-input v-model="form.siteSubtitle" /></el-form-item>
      <el-form-item label="备案号"><el-input v-model="form.icp" /></el-form-item>
      <el-form-item label="社交链接 JSON"><el-input v-model="form.socialLinks" type="textarea" :rows="3" /></el-form-item>
      <el-form-item label="关于我">
        <MdEditor v-model="form.aboutMd" language="zh-CN" style="width:100%" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="submit">保存</el-button>
      </el-form-item>
    </el-form>

    <el-card header="修改管理员密码" style="max-width: 520px; margin-top: 24px">
      <el-form label-width="90px">
        <el-form-item label="原密码"><el-input v-model="pwd.oldPassword" type="password" show-password /></el-form-item>
        <el-form-item label="新密码"><el-input v-model="pwd.newPassword" type="password" show-password /></el-form-item>
        <el-form-item>
          <el-button type="warning" @click="submitPwd">更新密码</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>
