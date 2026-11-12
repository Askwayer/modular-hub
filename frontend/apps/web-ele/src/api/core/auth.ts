import { requestClient } from '#/api/request';

export namespace AuthApi {
  export interface CaptchaValue {
    captchaCode: string;
    captchaEncryptData: string;
    captchaMix?: string;
    captchaUuid: string;
  }

  /** 登录接口参数 */
  export interface LoginParams {
    captcha?: CaptchaValue;
    password: string;
    username: string;
  }

  /** 登录接口返回值 */
  export interface LoginResult {
    expiresIn: number;
    nickName: string;
    tokenName: string;
    tokenPrefix: string;
    tokenValue: string;
    userId: number;
    username: string;
  }

  export interface LoginUserInfo {
    avatarUrl?: string;
    deptId?: number;
    deptName?: string;
    id: number;
    isSuperAdmin?: number;
    isSystem?: number;
    nickName: string;
    token?: string;
    tokenName?: string;
    tokenPrefix?: string;
    username: string;
  }

  export interface InfoResult {
    permissionCodes: string[];
    roleCodes: string[];
    userInfo: LoginUserInfo;
  }

  export interface CaptchaResult {
    captchaData?: unknown;
    captchaImg: string;
    captchaType: string;
    encryptData: string;
    uuid: string;
  }
}

/**
 * 登录
 */
export async function loginApi(data: AuthApi.LoginParams) {
  const { captcha, password, username } = data;
  return requestClient.post<AuthApi.LoginResult>('/auth/login', {
    captchaCode: captcha?.captchaCode,
    captchaEncryptData: captcha?.captchaEncryptData,
    captchaMix: captcha?.captchaMix || captcha?.captchaUuid,
    captchaUuid: captcha?.captchaUuid,
    password,
    userName: username,
  });
}

/**
 * 获取登录验证码
 */
export async function getCaptchaApi() {
  return requestClient.get<AuthApi.CaptchaResult>('/captcha/captcha');
}

/**
 * 获取当前登录用户、角色和权限信息
 */
export async function getAuthInfoApi() {
  return requestClient.get<AuthApi.InfoResult>('/auth/info');
}

/**
 * 退出登录
 */
export async function logoutApi() {
  return requestClient.post<boolean>('/auth/logout');
}
