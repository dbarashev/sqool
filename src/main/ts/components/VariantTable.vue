<template>
    <div class="list-group">
        <a href="#" class="list-group-item list-group-item-action"
           v-for="variant in variants" @click="makeActive(variant)"
           v-bind:class="{ 'active': variant.active }">
            {{ variant.name }}
        </a>
    </div>
</template>

<script lang="ts" src="./VariantTable.ts">
</script>
