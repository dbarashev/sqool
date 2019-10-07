<template>
    <div>
        <div id="review-toolbar" class="d-flex">
            <button type="button" class="btn btn-sm mr-3" @click="backToStudents">Назад</button>
            <button type="button" class="btn btn-sm mr-3" @click="getAttempt">Загрузить решение</button>
            <button type="button" class="btn btn-sm mr-3" @click="getLastReview">Загрузить последнюю рецензию</button>
            <button type="button" class="btn btn-sm btn-primary" @click="save">Сохранить рецензию</button>
        </div>
        <TaskMarkdown ref="taskMarkdown"></TaskMarkdown>
    </div>
</template>
<script lang="ts" src="./ReviewPage.ts"></script>
