import { Component, Vue } from 'vue-property-decorator';
import {ColumnSpec, TaskDto} from '../Task';

@Component
export default class TaskTable extends Vue {
    public tasks: TaskDto[] = [];
    public selectedTasks: TaskDto[] = [];

    public mounted() {
        $.ajax({
            url: '/admin/task/all',
        }).done((tasks: TaskDto[]) => {
            this.tasks = tasks;
        });
    }

    public taskResultSpec(task: TaskDto) {
        return JSON.parse(task.result_json)
            .map((column: ColumnSpec) => `${column.name} ${column.type}`).join(',');
    }

    public buildVariant() {
        const jsonTasks = this.selectedTasks.map((task) => ({
                name: task.name,
                keyAttributes: JSON.parse(task.result_json),
                nonKeyAttributes: [],
                solution: 'Put teacher\'s query here',
            }),
        );
        $.post('/admin/variant/new', {
            course: 'course',
            module: 'module',
            variant: 'variant',
            schema: 'schema',
            tasks: JSON.stringify(jsonTasks),
        });
    }
}
