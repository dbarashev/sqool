<template>
    <div id="contest-toolbar" class="d-flex">
        <button type="button" class="btn btn-raised btn-danger mr-3" @click="createNewContest">Новый Контест</button>
        <button type="button" class="btn btn-secondary mr-3" @click="editContest">Редактировать свойства</button>
        <button type="button" class="btn btn-secondary" @click="buildContest">Построить контест</button>
    </div>
</template>

<script lang="ts" src="./ContestToolbar.ts">
</script>
