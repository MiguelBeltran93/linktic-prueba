import { stockApi } from 'boot/axios'

export const getStock = (productId) =>
  stockApi.get(`/api/v1/stock/${productId}`).then(r => r.data.data)

export const purchase = (data) =>
  stockApi.post('/api/v1/stock/purchase', data).then(r => r.data.data)

export const adjustStock = (productId, quantity) =>
  stockApi.put(`/api/v1/stock/${productId}/adjust`, null, { params: { quantity } }).then(r => r.data.data)
