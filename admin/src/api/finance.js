import request from './request'

export function getTransactionList(params) {
  return request.get('/admin/finance/transactions', { params })
}

export function updateTransactionStatus(id, status) {
  return request.put(`/admin/finance/transactions/${id}/status`, { status })
}

export function getFinanceOverview() {
  return request.get('/admin/finance/overview')
}

