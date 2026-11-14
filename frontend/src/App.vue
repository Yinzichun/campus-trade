<template>
  <AppHeader v-if="$route.path !== '/login'" />
  <router-view />
</template>

<script setup>
import AppHeader from './components/AppHeader.vue'
</script>
