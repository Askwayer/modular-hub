<script lang="ts" setup>
import type { VbenFormSchema } from '@vben/common-ui';

import { computed, markRaw, ref } from 'vue';

import { AuthenticationLogin, z } from '@vben/common-ui';
import { $t } from '@vben/locales';

import { useAuthStore } from '#/store';

import LoginCaptcha from './login-captcha.vue';

defineOptions({ name: 'Login' });

const authStore = useAuthStore();

const captchaRefreshKey = ref(0);

async function handleSubmit(values: Record<string, any>) {
  try {
    await authStore.authLogin(values);
  } catch (error) {
    captchaRefreshKey.value += 1;
    throw error;
  }
}

const formSchema = computed((): VbenFormSchema[] => {
  return [
    {
      component: 'VbenInput',
      componentProps: {
        placeholder: $t('authentication.usernameTip'),
      },
      fieldName: 'username',
      label: $t('authentication.username'),
      rules: z
        .string()
        .min(4, { message: '用户名长度不能少于 4 位' })
        .max(32, { message: '用户名长度不能超过 32 位' }),
    },
    {
      component: 'VbenInputPassword',
      componentProps: {
        placeholder: $t('authentication.password'),
      },
      fieldName: 'password',
      label: $t('authentication.password'),
      rules: z
        .string()
        .min(8, { message: '密码长度不能少于 8 位' })
        .max(64, { message: '密码长度不能超过 64 位' }),
    },
    {
      component: markRaw(LoginCaptcha),
      componentProps: {
        refreshKey: captchaRefreshKey.value,
      },
      fieldName: 'captcha',
      rules: z.object({
        captchaCode: z.string().min(1, { message: '请输入验证码' }),
        captchaEncryptData: z.string().min(1),
        captchaMix: z.string().optional(),
        captchaUuid: z.string().min(1),
      }),
    },
  ];
});
</script>

<template>
  <AuthenticationLogin
    :form-schema="formSchema"
    :loading="authStore.loginLoading"
    @submit="handleSubmit"
  />
</template>
