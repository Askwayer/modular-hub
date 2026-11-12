<script setup lang="ts">
import type { AuthApi } from '#/api';

import { onMounted, ref, watch } from 'vue';

import { getCaptchaApi } from '#/api';

const props = defineProps<{
  refreshKey?: number;
}>();

const modelValue = defineModel<AuthApi.CaptchaValue>();

const captchaImage = ref('');
const loading = ref(false);

async function refreshCaptcha() {
  if (loading.value) return;

  loading.value = true;
  try {
    const captcha = await getCaptchaApi();
    captchaImage.value = captcha.captchaImg;
    modelValue.value = {
      captchaCode: '',
      captchaEncryptData: captcha.encryptData,
      captchaMix: captcha.uuid,
      captchaUuid: captcha.uuid,
    };
  } finally {
    loading.value = false;
  }
}

function updateCaptchaCode(event: Event) {
  const captchaCode = (event.target as HTMLInputElement).value;
  if (!modelValue.value) return;
  modelValue.value = { ...modelValue.value, captchaCode };
}

watch(() => props.refreshKey, refreshCaptcha);
onMounted(refreshCaptcha);
</script>

<template>
  <div class="flex h-10 w-full gap-2">
    <input
      :value="modelValue?.captchaCode || ''"
      autocomplete="off"
      class="h-10 min-w-0 flex-1 rounded-md border border-input bg-background px-3 text-sm outline-none focus:border-primary"
      placeholder="请输入验证码"
      @input="updateCaptchaCode"
    />
    <button
      class="h-10 w-28 shrink-0 overflow-hidden rounded-md border border-input bg-background"
      title="点击刷新验证码"
      type="button"
      @click="refreshCaptcha"
    >
      <span v-if="loading" class="text-xs text-muted-foreground">加载中...</span>
      <img
        v-else-if="captchaImage"
        alt="登录验证码"
        class="h-full w-full object-cover"
        :src="captchaImage"
      />
      <span v-else class="text-xs text-muted-foreground">点击刷新</span>
    </button>
  </div>
</template>
