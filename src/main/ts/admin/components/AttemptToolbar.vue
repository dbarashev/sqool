<template>
    <div id="attempt-toolbar" class="d-flex">
        <button type="button" class="btn btn-secondary" @click="showAttemptsByTasks">По задачам</button>
        <button type="button" class="btn btn-secondary" @click="showAttemptsByStudents">По студентам</button>
        <button type="button" class="btn btn-secondary mr-3" @click="showContests">Контесты</button>
    </div>
</template>

<script lang="ts" src="./AttemptToolbar.ts">
</script>