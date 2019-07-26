<template>
    <table class="table table-hover">
        <thead class="thead-dark">
        <tr>
            <th scope="col">Код</th>
            <th scope="col">Название</th>
            <th scope="col">Начало</th>
            <th scope="col">Окончание</th>
        </tr>
        </thead>
        <tbody>
        <tr v-for="c in contests"  @click="makeActive(c)" v-bind:class="{ 'table-active': c.active }">
            <td>{{ c.code }}</td>
            <td>{{ c.name }}</td>
            <td>{{ c.start_ts }}</td>
            <td>{{ c.end_ts }}</td>
        </tr>
        </tbody>
    </table>
</template>

<script lang="ts" src="./ContestTable.ts">
</script>

<!-- Add "scoped" attribute to limit CSS to this component only -->
<style scoped lang="scss">
    table {
        margin-top: 20px;
    }
</style>
