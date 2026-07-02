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

/**
 * 修改密码
 * POST /api/auth/change-password
 *
 * @param {Object} params - { oldPassword, newPassword, confirmPassword }
 * @returns {Promise<Object>} ResponseVO
 */
export function changePassword({ oldPassword, newPassword, confirmPassword }) {
  return request.post('/auth/changePassword', { oldPassword, newPassword, confirmPassword })
}