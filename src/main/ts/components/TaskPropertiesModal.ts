import { Component, Vue } from 'vue-property-decorator';
import {getTaskResultSql, TaskDto} from '../Task';
import TaskMarkdown from './TaskMarkdown';

@Component({
    components: {
        TaskMarkdown,
    },
})
export default class TaskPropertiesModal extends Vue {
    public taskName: string = '';
    public taskResult: string = '';
    public taskSolution: string = '';

    private taskId: number = -1;
    private deferred: JQueryDeferred<TaskDto> | undefined;
    get markdown(): TaskMarkdown {
        return this.$refs.taskMarkdown as TaskMarkdown;
    }


    public show(task: TaskDto): JQueryDeferred<TaskDto> {
        $('#task-properties').modal();
        this.taskId = task.id;
        this.taskName = task.name;
        this.taskResult = getTaskResultSql(task);
        this.markdown.textValue = task.description;

        this.taskSolution = task.solution;

        this.deferred = $.Deferred<TaskDto>();
        return this.deferred;
    }

    public hide() {
        $('#task-properties').modal('hide');
    }

    public submit() {
        if (this.deferred) {
            const taskDescription = this.markdown.textValue;
            this.deferred.resolve(new TaskDto(
                this.taskId, this.taskName, taskDescription, this.taskResult, this.taskSolution));
        }
    }

}
