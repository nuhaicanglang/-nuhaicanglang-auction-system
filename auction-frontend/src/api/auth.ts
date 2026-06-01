import { request } from './request'
import type { CaptchaResult, LoginPayload, LoginResult, RegisterPayload, User } from '@/types/domain'

export function loginApi(data: LoginPayload) {
  return request<LoginResult>({
    url: '/api/system/users/login',
    method: 'POST',
    data,
    skipAuth: true,
  })
}

export function registerApi(data: RegisterPayload) {
  return request<User>({
    url: '/api/system/users/register',
    method: 'POST',
    data,
    skipAuth: true,
  })
}

export function currentUserApi() {
  return request<User>({
    url: '/api/system/users/me',
    method: 'GET',
  })
}

export function captchaApi() {
  return request<CaptchaResult>({
    url: '/api/system/users/captcha',
    method: 'GET',
    skipAuth: true,
    silent: true,
  })
}

export function logoutApi() {
  return request<void>({
    url: '/api/system/users/logout',
    method: 'POST',
    silent: true,
  })
}
