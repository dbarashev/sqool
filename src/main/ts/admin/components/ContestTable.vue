<template>
    <table class="table table-hover mt-2">
        <thead class="thead-dark">
        <tr>
            <th scope="col">Код</th>
            <th scope="col">Название</th>
            <th scope="col">Начало</th>
            <th scope="col">Окончание</th>
        </tr>
        </thead>
        <tbody>
        <tr v-for="c in contests"
            @click="makeActive(c)"
            @dblclick="editContest()"
            v-bind:class="{ 'table-active': c.active }">
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

