import request from './request'

/** 用户列表（AD2.1）：支持关键字、角色、部门、状态筛选与分页 */
export function fetchUsers(params) {
  return request.get('/admin/users', { params })
}

export function fetchUser(id) {
  return request.get(`/admin/users/${id}`)
}

/** 创建用户（AD2.2 / AD2.3）：PM 与 Member 走同一个接口 */
export function createUser(data) {
  return request.post('/admin/users', data)
}

/** 修改用户（AD2.4 / AD2.5）：改姓名、角色、部门、状态 */
export function updateUser(id, data) {
  return request.patch(`/admin/users/${id}`, data)
}

export function resetPassword(id, newPassword) {
  return request.post(`/admin/users/${id}/reset-password`, { newPassword })
}
