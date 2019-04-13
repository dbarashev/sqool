<template>
    <div class="modal" tabindex="-1" role="dialog" id="task-properties">
        <div class="modal-dialog" role="document">
            <div class="modal-content">
                <div class="modal-header">
                    <h5 class="modal-title">Задача {{ taskName }}</h5>
                    <button type="button" class="close" data-dismiss="modal" aria-label="Close">
                        <span aria-hidden="true">&times;</span>
                    </button>
                </div>
                <div class="modal-body">
                    <ul class="nav nav-tabs nav-fill" id="taskTabs" role="tablist">
                        <li class="nav-item">
                            <a class="nav-link active" id="main-properties-tab" data-toggle="tab" href="#main-properties" role="tab"
                               aria-controls="main-properties" aria-selected="true">Основные свойства</a>
                        </li>
                        <li class="nav-item">
                            <a class="nav-link" id="description-tab" data-toggle="tab" href="#description" role="tab"
                               aria-controls="description" aria-selected="false">Описание</a>
                        </li>
                    </ul>
                    <div class="tab-content" id="taskTabsContent">
                        <div class="tab-pane fade show active" id="main-properties" role="tabpanel" aria-labelledby="main-properties-tab">
                            <form>
                                <div class="form-group">
                                    <label for="task-properties-name">Название задачи</label>
                                    <input type="text" class="form-control" id="task-properties-name" aria-describedby="task-properties-name-help" v-model="taskName">
                                    <small id="task-properties-name-help"
                                           class="form-text text-muted">Обычно это номер.</small>
                                </div>
                                <div class="form-group">
                                    <label for="task-properties-name">Столбцы результата</label>
                                    <input type="text" class="form-control"
                                           id="task-properties-result"
                                           aria-describedby="task-properties-result-help"
                                           v-model="taskResult"
                                    >
                                    <small id="task-properties-result-help"
                                           class="form-text text-muted"><code>id INT, name TEXT, ...</code></small>
                                </div>
                            </form>
                        </div>
                        <div class="tab-pane fade" id="description" role="tabpanel" aria-labelledby="description-tab">
                            <TaskMarkdown ref="taskMarkdown"> </TaskMarkdown>
                        </div>
                    </div>
                </div>
                <div class="modal-footer">
                    <button type="button" class="btn btn-primary" @click="submit">OK</button>
                </div>
            </div>
        </div>
    </div>
</template>

<script lang="ts">
import { Component, Vue } from 'vue-property-decorator';
import {TaskDto} from '../Task';
import TaskMarkdown from './TaskMarkdown.vue';

@Component({
  components: {
    TaskMarkdown,
  },
})
export default class TaskPropertiesModal extends Vue {
    public taskName: string = '';
    public taskResult: string = '';

    private taskId: number = -1;
    private deferred: JQueryDeferred<TaskDto> | undefined;

    public show(task: TaskDto): JQueryDeferred<TaskDto> {
        $('#task-properties').modal();
        this.taskId = task.id;
        this.taskName = task.name;
        this.taskResult = task.result_json;

        this.deferred = $.Deferred<TaskDto>();
        return this.deferred;
    }

    public hide() {
        $('#task-properties').modal('hide');
    }

    public submit() {
        if (this.deferred) {
            const markdown = this.$refs.taskMarkdown as TaskMarkdown;
            const taskDescription = markdown.markdownText() as string;
            this.deferred.resolve(new TaskDto(this.taskId, this.taskName, taskDescription, this.taskResult));
        }
    }
}
</script>
