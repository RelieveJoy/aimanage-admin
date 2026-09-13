import request from './request'

/** 部门树。根节点是公司本身 */
export function fetchDepartmentTree() {
  return request.get('/admin/departments')
}

export function createDepartment(data) {
  return request.post('/admin/departments', data)
}

export function updateDepartment(id, data) {
  return request.patch(`/admin/departments/${id}`, data)
}

export function deleteDepartment(id) {
  return request.delete(`/admin/departments/${id}`)
}
