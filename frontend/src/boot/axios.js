import axios from 'axios'

const catalogApi = axios.create({ baseURL: 'http://localhost:8080' })
const stockApi   = axios.create({ baseURL: 'http://localhost:8081' })

const apiKeyInterceptor = config => {
  config.headers['X-API-KEY'] = 'secret123'
  return config
}

catalogApi.interceptors.request.use(apiKeyInterceptor)
stockApi.interceptors.request.use(apiKeyInterceptor)

export { catalogApi, stockApi }
