import request from './request'

/* ---------------- 加人申请审批（AD8） ---------------- */

export function fetchRequests(params) {
  return request.get('/admin/requests', { params })
}

export function fetchPendingCount() {
  return request.get('/admin/requests/pending-count')
}

export function approveRequest(id, comment) {
  return request.post(`/admin/requests/${id}/approve`, { comment })
}

export function rejectRequest(id, comment) {
  return request.post(`/admin/requests/${id}/reject`, { comment })
}

/* ---------------- 通知中心 ---------------- */

export function fetchNotifications(params) {
  return request.get('/admin/notifications', { params })
}

export function fetchUnreadCount() {
  return request.get('/admin/notifications/unread-count')
}

export function markNotificationRead(id) {
  return request.post(`/admin/notifications/${id}/read`)
}

export function markAllNotificationsRead() {
  return request.post('/admin/notifications/read-all')
}

export const REQUEST_STATUS = {
  PENDING: { label: '待审批', type: 'warning' },
  APPROVED: { label: '已批准', type: 'success' },
  REJECTED: { label: '已拒绝', type: 'danger' },
}
