<template>
    <div class="d-flex">
        <button type="button" class="btn btn-raised btn-danger mr-3" @click="createNewTask">Новая Задача</button>
        <button type="button" class="btn btn-secondary" @click="buildVariant">Построить вариант</button>
    </div>
</template>

<script lang="ts">
import {Component, Inject, Vue} from 'vue-property-decorator';
import {TaskDto} from '../Task';
import TaskPropertiesModal from './TaskPropertiesModal.vue';
import TaskTable from "./TaskTable.vue";

@Component
export default class TaskToolbar extends Vue {
    @Inject() public readonly taskProperties!: () => TaskPropertiesModal;
    @Inject() public readonly taskTable!: () => TaskTable;

    public createNewTask() {
        const newTask = new TaskDto(-1, '000', '', '');
        this.taskProperties().show(newTask).then((updatedTask) => {
            return $.ajax('/admin/task/new', {
                method: 'POST',
                data: {
                    name: updatedTask.name,
                    description: updatedTask.description,
                    result: updatedTask.result_json,
                },
            });
        }).then(() => {
            this.taskProperties().hide();
        });
    }

    public buildVariant() {
        this.taskTable().buildVariant();
    }
}
</script>
