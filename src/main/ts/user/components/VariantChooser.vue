<template>
  <div v-if="isShown">
    <button v-if="isRandom" class="btn btn-outline-primary" @click="acceptRandom()">
      Дайте мне какой-нибудь вариант
    </button>
    <div v-if="!isRandom" class="d-flex flex-row justify-content-center">
      <div class="dropdown">
        <button class="btn btn-secondary dropdown-toggle" type="button"
           id="variantDropdown"
           data-toggle="dropdown"
           aria-haspopup="true"
           aria-expanded="false">
          {{ selectedVariantName }}
        </button>
        <div class="dropdown-menu" aria-labelledby="variantDropdown">
          <a class="dropdown-item" href="#"
             @click="selectVariant(v)"
             v-for="v in variants">{{ v.name }}</a>
        </div>
      </div>
      <button class="btn btn-outline-primary" @click="acceptSelected()">
        Выбираю этот!
      </button>
    </div>
  </div>
</template>

<script lang="ts" src="./VariantChooser.ts">
</script>

