<template>
    <div id="review-page">
        <TaskMarkdown ref="taskMarkdown"></TaskMarkdown>
        <button type="button" class="btn btn-raised btn-danger mr-3" @click="getAttempt">Get attempt</button>
        <button type="button" class="btn btn-raised btn-danger mr-3" @click="getLastReview">Get last review</button>
        <button type="button" class="btn btn-raised btn-danger mr-3" @click="save">Save</button>
    </div>
</template>
<script lang="ts" src="./ReviewPage.ts"></script>
