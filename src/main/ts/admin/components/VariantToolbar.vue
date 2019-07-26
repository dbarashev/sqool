<template>
    <div class="d-flex">
        <button type="button" class="btn btn-raised btn-danger mr-3" @click="createNewVariant">Новый Вариант</button>
        <button type="button" class="btn btn-secondary mr-3" @click="editVariant">Редактировать свойства</button>
    </div>
</template>

<script lang="ts" src="./VariantToolbar.ts"></script>