<template>
    <div>
        <table class="table table-hover mt-2">
            <thead class="thead-dark">
            <tr>
                <th scope="col">Название варианта</th>
                <th scope="col">Список задач</th>
            </tr>
            </thead>
            <tbody>
            <tr v-for="variant in variants"
                @click="makeActive(variant)"
                @dblclick="editVariant()"
                v-bind:class="{ 'table-active': variant.active }">
                <td>{{ variant.name }}</td>
                <td></td>
            </tr>
            </tbody>
        </table>
    </div>
</template>

<script lang="ts" src="./VariantTable.ts">
</script>
