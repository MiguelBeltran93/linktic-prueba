<template>
  <q-page padding>
    <div class="text-h5 q-mb-md">
      <q-icon name="list" class="q-mr-sm" />Catálogo de Productos
    </div>

    <q-table
      :rows="items"
      :columns="columns"
      row-key="id"
      :loading="loading"
      flat
      bordered
      :rows-per-page-options="[10, 20, 50]"
    >
      <template #top-right>
        <q-btn
          color="primary"
          icon="refresh"
          label="Actualizar"
          :loading="loading"
          @click="fetchItems"
        />
      </template>

      <template #body-cell-price="props">
        <q-td :props="props">
          ${{ props.row.price.toFixed(2) }}
        </q-td>
      </template>

      <template #body-cell-createdAt="props">
        <q-td :props="props">
          {{ formatDate(props.row.createdAt) }}
        </q-td>
      </template>

      <template #no-data>
        <div class="full-width text-center q-pa-md">
          <q-icon name="inbox" size="3em" color="grey" />
          <div class="text-grey q-mt-sm">No hay productos en el catálogo</div>
        </div>
      </template>
    </q-table>

    <q-banner v-if="error" class="bg-negative text-white q-mt-md" rounded>
      <template #avatar><q-icon name="error" /></template>
      {{ error }}
    </q-banner>
  </q-page>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { listItems } from 'src/api/catalogApi'

const items   = ref([])
const loading = ref(false)
const error   = ref(null)

const columns = [
  { name: 'id',          label: 'ID',          field: r => r.attributes.id,          align: 'left',  sortable: true },
  { name: 'name',        label: 'Nombre',       field: r => r.attributes.name,        align: 'left',  sortable: true },
  { name: 'price',       label: 'Precio',       field: r => r.attributes.price,       align: 'right', sortable: true },
  { name: 'description', label: 'Descripción',  field: r => r.attributes.description, align: 'left' },
  { name: 'createdAt',   label: 'Fecha',        field: r => r.attributes.createdAt,   align: 'center', sortable: true }
]

async function fetchItems () {
  loading.value = true
  error.value   = null
  try {
    items.value = await listItems()
  } catch (e) {
    error.value = 'No se pudo conectar con el catálogo. Verifica que el servicio esté activo.'
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

onMounted(fetchItems)
</script>
