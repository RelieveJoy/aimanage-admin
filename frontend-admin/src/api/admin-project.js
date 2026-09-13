import request from './request'

/* ---------------- 项目（AD4） ---------------- */

export function fetchProjects(params) {
  return request.get('/admin/projects', { params })
}

export function fetchProject(id) {
  return request.get(`/admin/projects/${id}`)
}

export function createProject(data) {
  return request.post('/admin/projects', data)
}

export function updateProject(id, data) {
  return request.patch(`/admin/projects/${id}`, data)
}

/* ---------------- 项目成员（AD5） ---------------- */

export function fetchProjectMembers(projectId) {
  return request.get(`/admin/projects/${projectId}/members`)
}

export function addProjectMember(projectId, userId) {
  return request.post(`/admin/projects/${projectId}/members`, { userId })
}

export function removeProjectMember(projectId, userId) {
  return request.delete(`/admin/projects/${projectId}/members/${userId}`)
}

/** 成员变更历史（AD5.3）—— 数据来自审计账本 */
export function fetchMemberHistory(projectId) {
  return request.get(`/admin/projects/${projectId}/member-history`)
}
