<script setup lang="ts">
defineProps<{
  label: string
  value: string | number
  caption?: string
}>()
</script>

<template>
  <article class="dashboard-card">
    <span>{{ label }}</span>
    <strong>{{ value }}</strong>
    <p v-if="caption">{{ caption }}</p>
  </article>
</template>
