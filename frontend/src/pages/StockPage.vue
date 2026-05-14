<template>
  <q-page padding>
    <div class="text-h5 q-mb-md">
      <q-icon name="inventory" class="q-mr-sm" />Gestión de Stock
    </div>

    <div class="row q-gutter-md">
      <!-- Consultar stock -->
      <div class="col-12 col-md-5">
        <q-card flat bordered>
          <q-card-section>
            <div class="text-subtitle1 q-mb-md">
              <q-icon name="search" class="q-mr-xs" />Consultar stock
            </div>
            <div class="row q-gutter-sm items-start">
              <div class="col">
                <q-input
                  v-model.number="queryId"
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
                  :loading="queryLoading"
                  :disable="!queryId"
                  style="height: 56px"
                  @click="fetchStock"
                />
              </div>
            </div>
          </q-card-section>

          <q-card-section v-if="stockData">
            <q-separator class="q-mb-md" />
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

          <q-card-section v-if="queryError">
            <q-banner class="bg-negative text-white" rounded>
              <template #avatar><q-icon name="error" /></template>
              {{ queryError }}
            </q-banner>
          </q-card-section>
        </q-card>
      </div>

      <!-- Ajustar stock -->
      <div class="col-12 col-md-5">
        <q-card flat bordered>
          <q-card-section>
            <div class="text-subtitle1 q-mb-md">
              <q-icon name="tune" class="q-mr-xs" />Ajustar stock
            </div>
            <div class="q-gutter-md">
              <q-input
                v-model.number="adjustId"
                type="number"
                label="ID de Producto"
                filled
                :min="1"
              >
                <template #prepend><q-icon name="tag" /></template>
              </q-input>

              <q-input
                v-model.number="adjustQty"
                type="number"
                label="Nueva cantidad"
                filled
                :min="0"
                hint="Establece el stock total disponible para este producto"
              >
                <template #prepend><q-icon name="add_box" /></template>
              </q-input>

              <q-btn
                color="secondary"
                icon="tune"
                label="Actualizar stock"
                :loading="adjustLoading"
                :disable="!adjustId || adjustQty === null || adjustQty === undefined"
                class="full-width"
                @click="submitAdjust"
              />
            </div>
          </q-card-section>

          <q-card-section v-if="adjustResult">
            <q-separator class="q-mb-md" />
            <q-banner class="bg-positive text-white" rounded>
              <template #avatar><q-icon name="check_circle" /></template>
              Stock del producto {{ adjustResult.attributes.productId }} actualizado a
              <strong>{{ adjustResult.attributes.quantity }} unidades</strong>
            </q-banner>
          </q-card-section>

          <q-card-section v-if="adjustError">
            <q-banner class="bg-negative text-white" rounded>
              <template #avatar><q-icon name="error" /></template>
              {{ adjustError }}
            </q-banner>
          </q-card-section>
        </q-card>
      </div>
    </div>
  </q-page>
</template>

<script setup>
import { ref } from 'vue'
import { getStock, adjustStock } from 'src/api/stockApi'

const queryId      = ref(null)
const stockData    = ref(null)
const queryLoading = ref(false)
const queryError   = ref(null)

const adjustId      = ref(null)
const adjustQty     = ref(null)
const adjustLoading = ref(false)
const adjustResult  = ref(null)
const adjustError   = ref(null)

async function fetchStock () {
  if (!queryId.value) return
  queryLoading.value = true
  stockData.value    = null
  queryError.value   = null
  try {
    stockData.value = await getStock(queryId.value)
  } catch (e) {
    const detail = e.response?.data?.errors?.[0]?.detail
    queryError.value = detail || 'No se pudo consultar el stock. Verifica que el servicio esté activo.'
  } finally {
    queryLoading.value = false
  }
}

async function submitAdjust () {
  if (!adjustId.value || adjustQty.value === null || adjustQty.value === undefined) return
  adjustLoading.value = true
  adjustResult.value  = null
  adjustError.value   = null
  try {
    adjustResult.value = await adjustStock(adjustId.value, adjustQty.value)
  } catch (e) {
    const detail = e.response?.data?.errors?.[0]?.detail
    adjustError.value = detail || 'No se pudo actualizar el stock.'
  } finally {
    adjustLoading.value = false
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
