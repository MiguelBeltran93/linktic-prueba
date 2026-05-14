import { catalogApi } from 'boot/axios'

export const listItems = () =>
  catalogApi.get('/api/v1/catalog/items').then(r => r.data.data)

export const getItem = (id) =>
  catalogApi.get(`/api/v1/catalog/items/${id}`).then(r => r.data.data)

export const createItem = (data) =>
  catalogApi.post('/api/v1/catalog/items', data).then(r => r.data.data)
