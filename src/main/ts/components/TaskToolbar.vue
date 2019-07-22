<template>
    <div class="d-flex">
        <button type="button" class="btn btn-raised btn-danger mr-3" @click="createNewTask">Новая Задача</button>
        <button type="button" class="btn btn-secondary mr-3" @click="editTask">Редактировать свойства</button>
        <button type="button" class="btn btn-secondary" @click="showAvailableSolutions">Рецензировать решения</button>
        <button type="button" class="btn btn-secondary" @click="showTasks">Список задач</button>
    </div>
</template>

<script lang="ts" src="./TaskToolbar.ts"></script>
