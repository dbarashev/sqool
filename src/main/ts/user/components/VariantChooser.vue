<template>
  <div v-if="isShown">
    <button v-if="isRandom" class="btn btn-outline-primary" @click="acceptRandom()">
      Выбрать случайный вариант
    </button>
    <div v-if="!isRandom">

      Выбрать неслучайный вариант

    </div>
  </div>
</template>

<script lang="ts" src="./VariantChooser.ts">
</script>

