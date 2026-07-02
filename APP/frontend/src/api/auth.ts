import request from '@/utils/request'
import type { Result } from '@/types'

export interface LoginDTO {
  username: string
  password: string
}

export interface LoginVO {
  token: string
  userId: number
  username: string
  nickname: string
}

export interface RegisterDTO {
  username: string
  password: string
  nickname?: string
  phone?: string
  email?: string
}

export function login(data: LoginDTO): Promise<Result<LoginVO>> {
  return request.post('/auth/login', data)
}

export function register(data: RegisterDTO): Promise<Result<number>> {
  return request.post('/auth/register', data)
}

export function logout(): Promise<Result<void>> {
  return request.post('/auth/logout')
}