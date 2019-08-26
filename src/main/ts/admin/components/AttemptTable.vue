<template>
    <table class="table table-hover table-striped">
        <thead class="thead-dark">
        <tr>
            <th>Задача</th>
            <th></th>
        </tr>
        </thead>
        <tbody>
        <tr v-for="attempt in attempts" v-if="attempt.status !== 'virgin'" @click="showReviewPage(attempt)">
            <td>{{ attempt.name }}</td>
            <td>
                <div v-if="attempt.status === 'failure'">
                    <i class="fa fa-thumbs-down"></i>Решена неверно
                </div>
                <div v-if="attempt.status === 'testing'">
                    <i class="fa fa-cog fa-spin fa-fw"></i>Проверяется...
                </div>
                <div v-if="attempt.status === 'success'">
                    <i class="fa fa-thumbs-up"></i>Решена!
                </div>
            </td>
        </tr>
        </tbody>
    </table>
</template>

<script lang="ts" src="./AttemptTable.ts">
</script>