<template>
  <q-page padding>
    <div class="text-h5 q-mb-md">
      <q-icon name="list" class="q-mr-sm" />Catálogo de Productos
    </div>

    <q-table
      :rows="filteredItems"
      :columns="columns"
      row-key="id"
      :loading="loading"
      flat
      bordered
      :rows-per-page-options="[10, 20, 50]"
    >
      <template #top-left>
        <q-input
          v-model="search"
          dense
          filled
          placeholder="Buscar por nombre..."
          style="width: 240px"
          clearable
        >
          <template #prepend><q-icon name="search" /></template>
        </q-input>
      </template>

      <template #top-right>
        <div class="q-gutter-sm">
          <q-btn
            color="primary"
            icon="add"
            label="Nuevo producto"
            @click="openCreateDialog"
          />
          <q-btn
            flat
            color="primary"
            icon="refresh"
            label="Actualizar"
            :loading="loading"
            @click="fetchItems"
          />
        </div>
      </template>

      <template #body-cell-price="props">
        <q-td :props="props">
          ${{ props.row.attributes.price.toFixed(2) }}
        </q-td>
      </template>

      <template #body-cell-createdAt="props">
        <q-td :props="props">
          {{ formatDate(props.row.attributes.createdAt) }}
        </q-td>
      </template>

      <template #body-cell-actions="props">
        <q-td :props="props" auto-width>
          <q-btn
            flat
            round
            dense
            icon="visibility"
            color="primary"
            @click="openDetail(props.row.attributes.id)"
          >
            <q-tooltip>Ver detalle</q-tooltip>
          </q-btn>
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

    <!-- Dialog: Crear producto -->
    <q-dialog v-model="showCreate" persistent>
      <q-card style="min-width: 420px">
        <q-card-section class="row items-center">
          <div class="text-h6"><q-icon name="add_box" class="q-mr-sm" />Nuevo producto</div>
          <q-space />
          <q-btn icon="close" flat round dense v-close-popup />
        </q-card-section>

        <q-separator />

        <q-card-section class="q-gutter-md">
          <q-input
            v-model="form.name"
            label="Nombre *"
            filled
            :rules="[v => !!v || 'El nombre es requerido']"
            ref="nameRef"
          />
          <q-input
            v-model.number="form.price"
            label="Precio *"
            type="number"
            filled
            :min="0"
            :rules="[v => v > 0 || 'El precio debe ser mayor a 0']"
            prefix="$"
          />
          <q-input
            v-model="form.description"
            label="Descripción (opcional)"
            filled
            type="textarea"
            autogrow
          />
        </q-card-section>

        <q-card-actions align="right" class="q-pb-md q-pr-md">
          <q-btn flat label="Cancelar" v-close-popup />
          <q-btn
            color="primary"
            label="Guardar"
            icon="save"
            :loading="saving"
            @click="submitCreate"
          />
        </q-card-actions>

        <q-banner v-if="createError" class="bg-negative text-white q-ma-md" rounded>
          <template #avatar><q-icon name="error" /></template>
          {{ createError }}
        </q-banner>
      </q-card>
    </q-dialog>

    <!-- Dialog: Detalle de producto -->
    <q-dialog v-model="showDetail">
      <q-card style="min-width: 380px">
        <q-card-section class="row items-center">
          <div class="text-h6"><q-icon name="info" class="q-mr-sm" />Detalle del producto</div>
          <q-space />
          <q-btn icon="close" flat round dense v-close-popup />
        </q-card-section>

        <q-separator />

        <q-card-section v-if="detailLoading" class="text-center q-pa-lg">
          <q-spinner size="40px" color="primary" />
        </q-card-section>

        <q-card-section v-else-if="detail">
          <q-list bordered rounded>
            <q-item>
              <q-item-section avatar><q-icon name="tag" color="primary" /></q-item-section>
              <q-item-section>
                <q-item-label caption>ID</q-item-label>
                <q-item-label>{{ detail.attributes.id }}</q-item-label>
              </q-item-section>
            </q-item>
            <q-item>
              <q-item-section avatar><q-icon name="label" color="primary" /></q-item-section>
              <q-item-section>
                <q-item-label caption>Nombre</q-item-label>
                <q-item-label>{{ detail.attributes.name }}</q-item-label>
              </q-item-section>
            </q-item>
            <q-item>
              <q-item-section avatar><q-icon name="attach_money" color="positive" /></q-item-section>
              <q-item-section>
                <q-item-label caption>Precio</q-item-label>
                <q-item-label class="text-positive text-weight-bold">${{ detail.attributes.price.toFixed(2) }}</q-item-label>
              </q-item-section>
            </q-item>
            <q-item v-if="detail.attributes.description">
              <q-item-section avatar><q-icon name="description" color="grey" /></q-item-section>
              <q-item-section>
                <q-item-label caption>Descripción</q-item-label>
                <q-item-label>{{ detail.attributes.description }}</q-item-label>
              </q-item-section>
            </q-item>
            <q-item>
              <q-item-section avatar><q-icon name="schedule" color="grey" /></q-item-section>
              <q-item-section>
                <q-item-label caption>Creado</q-item-label>
                <q-item-label>{{ formatDate(detail.attributes.createdAt) }}</q-item-label>
              </q-item-section>
            </q-item>
          </q-list>
        </q-card-section>

        <q-card-actions align="right" class="q-pb-md q-pr-md">
          <q-btn flat label="Cerrar" v-close-popup />
        </q-card-actions>
      </q-card>
    </q-dialog>
  </q-page>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { listItems, getItem, createItem } from 'src/api/catalogApi'
import { useQuasar } from 'quasar'

const $q = useQuasar()

const items   = ref([])
const loading = ref(false)
const error   = ref(null)
const search  = ref('')

const showCreate  = ref(false)
const saving      = ref(false)
const createError = ref(null)
const nameRef     = ref(null)
const form        = ref({ name: '', price: null, description: '' })

const showDetail    = ref(false)
const detail        = ref(null)
const detailLoading = ref(false)

const columns = [
  { name: 'id',          label: 'ID',          field: r => r.attributes.id,          align: 'left',  sortable: true },
  { name: 'name',        label: 'Nombre',       field: r => r.attributes.name,        align: 'left',  sortable: true },
  { name: 'price',       label: 'Precio',       field: r => r.attributes.price,       align: 'right', sortable: true },
  { name: 'description', label: 'Descripción',  field: r => r.attributes.description, align: 'left' },
  { name: 'createdAt',   label: 'Fecha',        field: r => r.attributes.createdAt,   align: 'center', sortable: true },
  { name: 'actions',     label: 'Acciones',     field: 'actions',                     align: 'center' }
]

const filteredItems = computed(() => {
  if (!search.value) return items.value
  const q = search.value.toLowerCase()
  return items.value.filter(i => i.attributes.name.toLowerCase().includes(q))
})

async function fetchItems () {
  loading.value = true
  error.value   = null
  try {
    items.value = await listItems()
  } catch {
    error.value = 'No se pudo conectar con el catálogo. Verifica que el servicio esté activo.'
  } finally {
    loading.value = false
  }
}

function openCreateDialog () {
  form.value = { name: '', price: null, description: '' }
  createError.value = null
  showCreate.value = true
}

async function submitCreate () {
  if (!form.value.name || !form.value.price) return
  saving.value      = true
  createError.value = null
  try {
    await createItem({
      name:        form.value.name,
      price:       form.value.price,
      description: form.value.description || undefined
    })
    showCreate.value = false
    $q.notify({ type: 'positive', message: 'Producto creado correctamente', position: 'top' })
    await fetchItems()
  } catch (e) {
    const detail = e.response?.data?.errors?.[0]?.detail
    createError.value = detail || 'Error al crear el producto.'
  } finally {
    saving.value = false
  }
}

async function openDetail (id) {
  detail.value        = null
  detailLoading.value = true
  showDetail.value    = true
  try {
    detail.value = await getItem(id)
  } catch {
    showDetail.value = false
    $q.notify({ type: 'negative', message: 'No se pudo cargar el detalle del producto', position: 'top' })
  } finally {
    detailLoading.value = false
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
