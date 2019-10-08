<template>
    <div id="attempt-toolbar" class="d-flex">
        <button type="button" class="btn btn-secondary mr-3" @click="showContests">Назад</button>
        <button type="button"
                v-bind:class="{btn: true, 'btn-secondary': true, active:  activeView === 'by-tasks'}"
                @click="showAttemptsByTasks">По задачам</button>
        <button type="button"
                v-bind:class="{btn: true, 'btn-secondary': true, active:  activeView === 'by-students'}"
                @click="showAttemptsByStudents">По студентам</button>
    </div>
</template>

<script lang="ts" src="./AttemptToolbar.ts">
</script>
