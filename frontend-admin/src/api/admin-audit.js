import request from './request'

/** 全局审计检索（AD6）—— 只读 */
export function fetchAudits(params) {
  return request.get('/admin/audits', { params })
}

export function fetchAudit(id) {
  return request.get(`/admin/audits/${id}`)
}

/** 目标对象类型的中文名，列表与筛选共用 */
export const TARGET_TYPES = [
  { value: 'PROJECT', label: '项目' },
  { value: 'PROJECT_MEMBER', label: '项目成员' },
  { value: 'USER', label: '用户账号' },
  { value: 'DEPARTMENT', label: '部门' },
]

export const ACTION_LABELS = {
  CREATE: '新增',
  UPDATE: '修改',
  DELETE: '删除',
}

export const ACTION_TAG_TYPES = {
  CREATE: 'success',
  UPDATE: 'warning',
  DELETE: 'danger',
}

export function targetTypeLabel(value) {
  return TARGET_TYPES.find((t) => t.value === value)?.label || value
}
