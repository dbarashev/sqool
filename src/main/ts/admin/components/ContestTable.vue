<template>
    <table class="table table-hover mt-2">
        <thead class="thead-dark">
        <tr>
            <th scope="col">Код</th>
            <th scope="col">Название</th>
        </tr>
        </thead>
        <tbody>
        <tr v-for="c in contests"
            @click="makeActive(c)"
            @dblclick="editContest()"
            v-bind:class="{ 'table-active': c.active }">
            <td>{{ c.code }}</td>
            <td>
                <a class="btn float-right"
                   @click="showAttemptsModal(c)"
                   role="button">
                Решения
                </a><span>{{ c.name }}</span>
                <br><small class="text-muted">{{ c.start_ts }} - {{ c.end_ts }}</small>
            </td>
        </tr>
        </tbody>
    </table>
</template>

<script lang="ts" src="./ContestTable.ts">
</script>

