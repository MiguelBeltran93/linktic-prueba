<template>
  <q-layout view="lHh Lpr lFf">
    <q-header elevated>
      <q-toolbar>
        <q-btn flat dense round icon="menu" aria-label="Menu" @click="toggleLeftDrawer" />
        <q-toolbar-title>Linktic</q-toolbar-title>
      </q-toolbar>
    </q-header>

    <q-drawer v-model="leftDrawerOpen" show-if-above bordered>
      <q-list>
        <q-item-label header>Navegación</q-item-label>
        <q-item clickable v-ripple to="/" exact>
          <q-item-section avatar><q-icon name="list" /></q-item-section>
          <q-item-section>Catálogo</q-item-section>
        </q-item>
        <q-item clickable v-ripple to="/purchase">
          <q-item-section avatar><q-icon name="shopping_cart" /></q-item-section>
          <q-item-section>Compra</q-item-section>
        </q-item>
        <q-item clickable v-ripple to="/stock">
          <q-item-section avatar><q-icon name="inventory" /></q-item-section>
          <q-item-section>Stock</q-item-section>
        </q-item>
      </q-list>
    </q-drawer>

    <q-page-container>
      <router-view />
    </q-page-container>
  </q-layout>
</template>

<script setup>
import { ref } from 'vue'

const leftDrawerOpen = ref(false)
function toggleLeftDrawer () {
  leftDrawerOpen.value = !leftDrawerOpen.value
}
</script>
