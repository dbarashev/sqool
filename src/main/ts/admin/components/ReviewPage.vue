<template>
    <div>
        <h2 class="mb-2">Студент {{ attemptUserName }}. Задача {{ attemptTaskName }}</h2>
        <div id="review-toolbar" class="d-flex">
            <button type="button" class="btn btn-sm mr-3" @click="backToStudents">Назад</button>
            <button type="button" class="btn btn-sm mr-3" @click="getAttempt">Загрузить решение</button>
            <button type="button" class="btn btn-sm mr-3" @click="getLastReview">Загрузить последнюю рецензию</button>
            <button type="button" class="btn btn-sm btn-primary" @click="save">Сохранить рецензию</button>
            <button v-if="allReviews.length > 0"
                    type="button" class="btn btn-sm float-right"
                    data-toggle="collapse"
                    data-target="#allReviews"
                    aria-expanded="false"
                    aria-controls="allReviews">Другие рецензии ({{ allReviews.length }})</button>
            <button type="button" class="btn btn-sm float-right"
                    data-toggle="collapse"
                    data-target="#taskDescription"
                    aria-expanded="false"
                    aria-controls="allReviews">Описание задачи</button>
        </div>
        <div class="collapse" id="allReviews">
            <div class="card card-body" v-html="allReviewsHtml">
            </div>
        </div>
        <div class="collapse" id="taskDescription">
            <div class="card card-body" v-html="taskDescriptionHtml">
            </div>
        </div>
        <TaskMarkdown ref="taskMarkdown"></TaskMarkdown>
    </div>
</template>
<script lang="ts" src="./ReviewPage.ts"></script>
