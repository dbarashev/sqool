<template>
    <div>
    <div class="d-flex">
        <button type="button" class="btn btn-raised btn-danger mr-3" @click="createNewTask">Новая Задача</button>
        <button type="button" class="btn btn-secondary">Построить вариант</button>
    </div>

    </div>
</template>

<script lang="ts">
import {Component, Inject, Vue} from 'vue-property-decorator';

    @Component
export default class TaskToolbar extends Vue {
    @Inject() public readonly showTaskProperties!: () => void;

    public createNewTask() {
        // let modal = this.$refs.taskPropertiesModal as TaskPropertiesModal;
        // modal.show();
        this.showTaskProperties();
    }
}
</script>

<!-- Add "scoped" attribute to limit CSS to this component only -->
<style scoped lang="scss">
</style>
