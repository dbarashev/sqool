import { Component, Vue } from 'vue-property-decorator';
import {ColumnSpec, getTaskResultSql, TaskDto} from '../Task';

@Component
export default class TaskTable extends Vue {
    public tasks: TaskDto[] = [];
    public selectedTasks: TaskDto[] = [];
    private activeTask?: TaskDto;

    public mounted() {
        this.refresh();
    }

    public taskResultSpec(task: TaskDto): string {
        return getTaskResultSql(task);
    }

    public buildVariant() {
        const tasksId = this.selectedTasks.map(task => task.id);
        $.post('/admin/variant/new', {
            course: 'course',
            module: 'module',
            variant: 'variant',
            schema: 'schema',
            tasks: JSON.stringify(tasksId),
        });
    }

    public makeActive(task: TaskDto) {
        task.active = true;
        if (this.activeTask) {
            this.activeTask.active = false;
        }
        this.activeTask = task;
        this.$forceUpdate();
    }

    public getActiveTask(): TaskDto | undefined {
        return this.activeTask;
    }

    public refresh() {
        $.ajax({
            url: '/admin/task/all',
        }).done((tasks: TaskDto[]) => {
            this.tasks = [];
            tasks.forEach((t) => this.tasks.push(t));
        });
    }
}
