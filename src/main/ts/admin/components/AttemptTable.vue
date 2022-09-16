<template>
    <table class="table table-hover table-striped">
        <tbody>
        <tr v-for="attempt in attempts" v-if="attempt.status !== 'virgin'" @click="showReviewPage(attempt)">
            <td>
                <a class="btn float-right" role="button"
                   @click="showReviewPage(attempt)">
                    Рецензии
                </a>
                {{ attempt.name }}
                <br>
                <small class="text-muted">{{ formatStatus(attempt) }}</small>&nbsp;
                <small class="text-muted">Попыток: {{ attempt.count }}</small>&nbsp;
                <small class="text-muted">Последняя попытка: {{ formatTimestamp(attempt) }}</small>
            </td>
        </tr>
        </tbody>
    </table>
</template>

<script lang="ts" src="./AttemptTable.ts">
</script>
