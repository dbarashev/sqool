<template>
    <div>
        <table class="table table-hover mt-2">
            <thead class="thead-dark">
            <tr>
                <th scope="col">Задача</th>
                <th scope="col">Описание</th>
                <th scope="col">Столбцы результата</th>
            </tr>
            </thead>
            <tbody>
                <tr v-for="t in tasks"
                    @click="makeActive(t)"
                    @dblclick="editTask()"
                    v-bind:class="{ 'table-active': t.active }">
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

