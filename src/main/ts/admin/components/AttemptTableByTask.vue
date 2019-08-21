<template>
    <table class="table table-hover">
        <thead class="thead-dark">
        <tr>
            <th scope="col">Задача</th>
            <th scope="col">Успешные попытки</th>
            <th scope="col">Все попытки</th>
        </tr>
        </thead>
        <tbody>
        <tr v-for="task in attempts.keys()">
            <td>{{ task }}</td>
            <td>{{ attempts.get(task).filter(a => a.status === 'success').length }}</td>
            <td>{{ attempts.get(task).filter(a => a.status !== 'virgin').length }}</td>
        </tr>
        </tbody>
    </table>
</template>

<script lang="ts" src="./AttemptTableByTask.ts">
</script>