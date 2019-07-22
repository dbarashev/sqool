<template>
    <div>
        <table class="table table-hover">
            <thead class="thead-dark">
            <tr>
                <th scope="col">Задача</th>
                <th scope="col">Описание</th>
                <th scope="col">Столбцы результата</th>
            </tr>
            </thead>
            <tbody>
                <tr v-for="t in tasks" @click="makeActive(t)" v-bind:class="{ 'table-active': t.active }">
                    <td>{{ t.name }}</td>
                    <td v-html="t.description"></td>
                    <td>{{ taskResultSpec(t) }}</td>
                </tr>
            </tbody>
        </table>
    </div>
</template>

<script lang="ts" src="./TaskTable.ts">
</script>

<!-- Add "scoped" attribute to limit CSS to this component only -->
<style scoped lang="scss">
    table {
        margin-top: 20px;
    }
</style>
