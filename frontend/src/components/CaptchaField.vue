<script setup>
import { onMounted, reactive } from 'vue'
import { fetchCaptcha } from '../api'

const model = defineModel({ type: Object, required: true })

const state = reactive({
  image: '',
  loading: false,
})

async function refresh() {
  state.loading = true
  try {
    const res = await fetchCaptcha()
    const data = res.data || {}
    model.value.captchaId = data.captchaId || ''
    model.value.captchaCode = ''
    state.image = data.imageBase64 || ''
  } catch {
    state.image = ''
  } finally {
    state.loading = false
  }
}

onMounted(refresh)

defineExpose({ refresh })
</script>

<template>
  <div class="captcha-row">
    <input
      v-model="model.captchaCode"
      class="captcha-input"
      maxlength="8"
      placeholder="验证码"
      autocomplete="off"
      required
    />
    <button type="button" class="captcha-img-btn" :disabled="state.loading" title="点击刷新" @click="refresh">
      <img v-if="state.image" :src="state.image" alt="验证码" />
      <span v-else>{{ state.loading ? '加载中' : '刷新' }}</span>
    </button>
  </div>
</template>

<style scoped>
.captcha-row {
  display: flex;
  gap: 0.6rem;
  align-items: center;
}
.captcha-input {
  flex: 1;
  min-width: 0;
}
.captcha-img-btn {
  border: 1px solid var(--line, #ddd);
  background: var(--card, #fff);
  padding: 0;
  height: 40px;
  width: 120px;
  border-radius: 8px;
  overflow: hidden;
  cursor: pointer;
}
.captcha-img-btn img {
  display: block;
  width: 100%;
  height: 100%;
  object-fit: cover;
}
</style>
