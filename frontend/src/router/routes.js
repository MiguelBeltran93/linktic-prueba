const routes = [
  {
    path: '/',
    component: () => import('layouts/MainLayout.vue'),
    children: [
      { path: '',         component: () => import('pages/CatalogPage.vue') },
      { path: 'purchase', component: () => import('pages/PurchasePage.vue') },
      { path: 'stock',    component: () => import('pages/StockPage.vue') }
    ]
  },
  {
    path: '/:catchAll(.*)*',
    component: () => import('pages/ErrorNotFound.vue')
  }
]

export default routes
