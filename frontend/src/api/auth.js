import request from './request'

/**
 * 认证相关 API
 */

/**
 * 用户登录
 * POST /api/auth/login
 *
 * @param {string} username - 用户名/工号
 * @param {string} password - 密码
 * @returns {Promise<Object>} ResponseVO<LoginResponse> — { status, code, info, data: { token, userId, username, name, role } }
 */
export function login(username, password) {
  return request.post('/auth/login', { username, password })
}