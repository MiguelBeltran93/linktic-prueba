<template>
  <q-page padding>
    <div class="text-h5 q-mb-md">
      <q-icon name="inventory" class="q-mr-sm" />Consulta de Stock
    </div>

    <q-card flat bordered style="max-width: 520px">
      <q-card-section>
        <div class="text-subtitle1 q-mb-md">Buscar stock por producto</div>
        <div class="row q-gutter-sm items-start">
          <div class="col">
            <q-input
              v-model.number="productId"
              type="number"
              label="ID de Producto"
              filled
              :min="1"
              @keyup.enter="fetchStock"
            >
              <template #prepend><q-icon name="search" /></template>
            </q-input>
          </div>
          <div class="col-auto">
            <q-btn
              color="primary"
              icon="search"
              label="Consultar"
              :loading="loading"
              :disable="!productId"
              style="height: 56px"
              @click="fetchStock"
            />
          </div>
        </div>
      </q-card-section>

      <!-- Resultado -->
      <q-card-section v-if="stockData">
        <q-separator class="q-mb-md" />
        <div class="text-subtitle2 text-grey q-mb-sm">Resultado</div>
        <q-list bordered rounded>
          <q-item>
            <q-item-section avatar>
              <q-icon name="tag" color="primary" />
            </q-item-section>
            <q-item-section>
              <q-item-label caption>Producto ID</q-item-label>
              <q-item-label>{{ stockData.attributes.productId }}</q-item-label>
            </q-item-section>
          </q-item>

          <q-item>
            <q-item-section avatar>
              <q-icon
                :name="stockData.attributes.quantity > 0 ? 'check_circle' : 'warning'"
                :color="stockData.attributes.quantity > 0 ? 'positive' : 'warning'"
              />
            </q-item-section>
            <q-item-section>
              <q-item-label caption>Stock disponible</q-item-label>
              <q-item-label :class="stockData.attributes.quantity > 0 ? 'text-positive' : 'text-warning'">
                <span class="text-h6">{{ stockData.attributes.quantity }}</span> unidades
              </q-item-label>
            </q-item-section>
          </q-item>

          <q-item v-if="stockData.attributes.updatedAt">
            <q-item-section avatar>
              <q-icon name="schedule" color="grey" />
            </q-item-section>
            <q-item-section>
              <q-item-label caption>Última actualización</q-item-label>
              <q-item-label>{{ formatDate(stockData.attributes.updatedAt) }}</q-item-label>
            </q-item-section>
          </q-item>
        </q-list>

        <q-btn
          v-if="stockData.attributes.quantity > 0"
          color="secondary"
          icon="shopping_cart"
          label="Ir a comprar"
          flat
          class="q-mt-md"
          to="/purchase"
        />
      </q-card-section>

      <!-- Error -->
      <q-card-section v-if="error">
        <q-banner class="bg-negative text-white" rounded>
          <template #avatar><q-icon name="error" /></template>
          {{ error }}
        </q-banner>
      </q-card-section>
    </q-card>
  </q-page>
</template>

<script setup>
import { ref } from 'vue'
import { getStock } from 'src/api/stockApi'

const productId = ref(null)
const stockData = ref(null)
const loading   = ref(false)
const error     = ref(null)

async function fetchStock () {
  if (!productId.value) return
  loading.value  = true
  stockData.value = null
  error.value    = null
  try {
    stockData.value = await getStock(productId.value)
  } catch (e) {
    const detail = e.response?.data?.errors?.[0]?.detail
    error.value = detail || 'No se pudo consultar el stock. Verifica que el servicio esté activo.'
  } finally {
    loading.value = false
  }
}

function formatDate (iso) {
  if (!iso) return '—'
  return new Date(iso).toLocaleDateString('es-CO', {
    year: 'numeric', month: 'short', day: '2-digit',
    hour: '2-digit', minute: '2-digit'
  })
}
</script>
