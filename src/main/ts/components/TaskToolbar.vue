<template>
    <div class="d-flex">
        <button type="button" class="btn btn-raised btn-danger mr-3" @click="createNewTask">Новая Задача</button>
        <button type="button" class="btn btn-secondary mr-3" @click="editTask">Редактировать свойства</button>
        <button type="button" class="btn btn-secondary" @click="buildVariant">Построить вариант</button>
    </div>
</template>

<script lang="ts">
import {Component, Inject, Vue} from 'vue-property-decorator';
import {TaskDto} from '../Task';
import TaskPropertiesModal from './TaskPropertiesModal';
import TaskTable from './TaskTable';

@Component
export default class TaskToolbar extends Vue {

    private static buildTaskPayload(task: TaskDto): object {
        return {
            method: 'POST',
            data: {
                id: task.id,
                name: task.name,
                description: task.description,
                result: task.result_json,
                solution: task.solution,
            },
        };
    }
    @Inject() public readonly taskProperties!: () => TaskPropertiesModal;
    @Inject() public readonly taskTable!: () => TaskTable;

    public createNewTask() {
        const newTask = new TaskDto(-1, '000', '', '', '');
        this.taskProperties().show(newTask).then((updatedTask) => {
            return $.ajax('/admin/task/new', TaskToolbar.buildTaskPayload(updatedTask));
        }).then(() => {
            this.taskProperties().hide();
            this.taskTable().refresh();
        });
    }

    public editTask() {
        const activeTask = this.taskTable().getActiveTask();
        if (activeTask) {
            this.taskProperties().show(activeTask).then((updatedTask) => {
                $.ajax('/admin/task/update', TaskToolbar.buildTaskPayload(updatedTask));
            }).then(() => {
                this.taskProperties().hide();
                this.taskTable().refresh();
            });
        }
    }

    public buildVariant() {
        this.taskTable().buildVariant();
    }
}
</script>
