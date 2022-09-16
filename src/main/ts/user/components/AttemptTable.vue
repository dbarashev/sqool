<template>
    <table class="table table-hover table-striped table-fixed">
        <thead class="thead-dark">
        <tr>
            <th class="w-25">Задача</th>
            <th class="w-25">Сложность</th>
            <th class="w-25">Стоимость</th>
            <th class="w-25"></th>
        </tr>
        </thead>
        <tbody>
        <tr v-for="attempt in contest ? contest.attempts : []">
            <td @click="showTaskAttempt(attempt)">{{ attempt.taskEntity.name }}</td>
            <td @click="showTaskAttempt(attempt)">{{ attempt.taskEntity.difficulty }}</td>
            <td @click="showTaskAttempt(attempt)">{{ attempt.taskEntity.score }}</td>
            <td>
                <div v-if="attempt.status === 'failure'">
                    {{ getErrorMessage(attempt.count) }}
                </div>
                <div v-if="attempt.status === 'virgin'" @click="showTaskAttempt(attempt)">
                    Нет решения
                </div>
                <div v-if="attempt.status === 'testing'" @click="showTaskAttempt(attempt)">
                    <i class="fa fa-cog fa-spin fa-fw"></i>Проверяем...
                </div>
                <div v-if="attempt.status === 'success'" @click="showTaskAttempt(attempt)">
                    <i class="fa fa-thumbs-up"></i>Решена!
                </div>
            </td>
        </tr>
        </tbody>
    </table>
</template>

<script lang="ts" src="./AttemptTable.ts">
</script>

