import type { UserInfo } from '@vben/types';

import type { AuthApi } from './auth';

import { getAuthInfoApi } from './auth';

export function mapAuthInfoToUserInfo(info: AuthApi.InfoResult): UserInfo {
  const { roleCodes, userInfo } = info;
  return {
    avatar: userInfo.avatarUrl || '',
    desc: userInfo.deptName || '',
    homePath: '/analytics',
    realName: userInfo.nickName || userInfo.username,
    roles: roleCodes || [],
    token: userInfo.token || '',
    userId: String(userInfo.id),
    username: userInfo.username,
  };
}

/**
 * 获取用户信息
 */
export async function getUserInfoApi() {
  const authInfo = await getAuthInfoApi();
  return mapAuthInfoToUserInfo(authInfo);
}
