<template>
  <q-page padding>
    <div class="text-h5 q-mb-md">
      <q-icon name="shopping_cart" class="q-mr-sm" />Realizar Compra
    </div>

    <q-card flat bordered style="max-width: 680px; margin: 0 auto">
      <q-card-section>
        <q-stepper
          v-model="step"
          ref="stepper"
          color="primary"
          animated
          flat
        >
          <!-- PASO 1: Seleccionar producto -->
          <q-step :name="1" title="Seleccionar producto" icon="search" :done="step > 1">
            <q-select
              v-model="selectedProduct"
              :options="productOptions"
              label="Producto"
              option-label="label"
              option-value="value"
              emit-value
              map-options
              filled
              :loading="loadingProducts"
              :hint="selectedProduct ? `Precio: $${selectedProductPrice}` : 'Selecciona un producto del catálogo'"
            >
              <template #prepend><q-icon name="inventory_2" /></template>
              <template #no-option>
                <q-item>
                  <q-item-section class="text-grey">Sin productos disponibles</q-item-section>
                </q-item>
              </template>
            </q-select>

            <q-banner v-if="loadError" class="bg-negative text-white q-mt-md" rounded>
              <template #avatar><q-icon name="error" /></template>
              {{ loadError }}
            </q-banner>
          </q-step>

          <!-- PASO 2: Cantidad y confirmar -->
          <q-step :name="2" title="Cantidad y confirmar" icon="edit" :done="step > 2">
            <div class="q-gutter-md">
              <q-input
                v-model.number="units"
                type="number"
                label="Unidades"
                filled
                :min="1"
                :rules="[v => v >= 1 || 'Debe ser al menos 1']"
              >
                <template #prepend><q-icon name="add_shopping_cart" /></template>
              </q-input>

              <q-card flat bordered class="bg-dark q-pa-md">
                <div class="text-subtitle2 text-grey q-mb-sm">Resumen de compra</div>
                <div class="row q-gutter-sm">
                  <div class="col-12">
                    <span class="text-grey">Producto: </span>
                    <span class="text-white">{{ selectedProductName }}</span>
                  </div>
                  <div class="col-12">
                    <span class="text-grey">Precio unitario: </span>
                    <span class="text-white">${{ selectedProductPrice }}</span>
                  </div>
                  <div class="col-12">
                    <span class="text-grey">Unidades: </span>
                    <span class="text-white">{{ units }}</span>
                  </div>
                  <div class="col-12">
                    <span class="text-grey">Total estimado: </span>
                    <span class="text-primary text-weight-bold">${{ (selectedProductPrice * units).toFixed(2) }}</span>
                  </div>
                </div>
              </q-card>
            </div>
          </q-step>

          <!-- PASO 3: Resultado -->
          <q-step :name="3" title="Resultado" icon="check_circle">
            <div v-if="purchaseResult" class="text-center q-pa-md">
              <q-icon name="check_circle" color="positive" size="4em" />
              <div class="text-h6 text-positive q-mt-sm">¡Compra realizada con éxito!</div>
              <q-separator class="q-my-md" />
              <div class="text-body1">
                <div>Producto ID: <strong>{{ purchaseResult.attributes.productId }}</strong></div>
                <div>Unidades compradas: <strong>{{ purchaseResult.attributes.units }}</strong></div>
                <div>Stock restante: <strong class="text-primary">{{ purchaseResult.attributes.remainingStock }}</strong></div>
              </div>
            </div>

            <div v-if="purchaseError" class="text-center q-pa-md">
              <q-icon name="cancel" color="negative" size="4em" />
              <div class="text-h6 text-negative q-mt-sm">No se pudo completar la compra</div>
              <div class="text-body2 text-grey q-mt-sm">{{ purchaseError }}</div>
            </div>
          </q-step>

          <!-- Navegación -->
          <template #navigation>
            <q-stepper-navigation>
              <q-btn
                v-if="step < 3"
                color="primary"
                :label="step === 2 ? 'Confirmar compra' : 'Siguiente'"
                :loading="purchasing"
                :disable="step === 1 && !selectedProduct"
                @click="onNext"
              />
              <q-btn
                v-if="step > 1 && step < 3"
                flat
                color="primary"
                label="Atrás"
                class="q-ml-sm"
                @click="stepper.previous()"
              />
              <q-btn
                v-if="step === 3"
                color="primary"
                label="Nueva compra"
                icon="refresh"
                @click="resetStepper"
              />
            </q-stepper-navigation>
          </template>
        </q-stepper>
      </q-card-section>
    </q-card>
  </q-page>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { listItems } from 'src/api/catalogApi'
import { purchase } from 'src/api/stockApi'

const step           = ref(1)
const stepper        = ref(null)
const selectedProduct = ref(null)
const units          = ref(1)
const loadingProducts = ref(false)
const purchasing     = ref(false)
const loadError      = ref(null)
const purchaseResult = ref(null)
const purchaseError  = ref(null)
const products       = ref([])

const productOptions = computed(() =>
  products.value.map(p => ({
    label: `${p.attributes.name} — $${p.attributes.price}`,
    value: p.attributes.id
  }))
)

const selectedProductName = computed(() => {
  const p = products.value.find(p => p.attributes.id === selectedProduct.value)
  return p ? p.attributes.name : '—'
})

const selectedProductPrice = computed(() => {
  const p = products.value.find(p => p.attributes.id === selectedProduct.value)
  return p ? p.attributes.price : 0
})

async function fetchProducts () {
  loadingProducts.value = true
  loadError.value = null
  try {
    products.value = await listItems()
  } catch {
    loadError.value = 'No se pudo cargar el catálogo. Verifica que el servicio esté activo.'
  } finally {
    loadingProducts.value = false
  }
}

async function onNext () {
  if (step.value === 1) {
    stepper.value.next()
    return
  }
  if (step.value === 2) {
    await confirmPurchase()
  }
}

async function confirmPurchase () {
  purchasing.value  = true
  purchaseResult.value = null
  purchaseError.value  = null
  try {
    purchaseResult.value = await purchase({
      productId: selectedProduct.value,
      units: units.value
    })
    stepper.value.next()
  } catch (e) {
    const detail = e.response?.data?.errors?.[0]?.detail || 'Error inesperado al procesar la compra'
    purchaseError.value = detail
    stepper.value.next()
  } finally {
    purchasing.value = false
  }
}

function resetStepper () {
  step.value           = 1
  selectedProduct.value = null
  units.value          = 1
  purchaseResult.value = null
  purchaseError.value  = null
}

onMounted(fetchProducts)
</script>
