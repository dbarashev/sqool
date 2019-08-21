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
        <tr v-for="task in taskStats">
            <td>{{ task.task_name }}</td>
            <td>{{ task.solved }}</td>
            <td>{{ task.attempted }}</td>
        </tr>
        </tbody>
    </table>
</template>

<script lang="ts" src="./AttemptTableByTask.ts">
</script>