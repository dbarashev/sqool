import {Component, Inject, Vue} from 'vue-property-decorator';
import {TaskDto} from '../Task';
import TaskPropertiesModal from './TaskPropertiesModal';
import TaskTable from './TaskTable';
import AvailableSolutions from './AvailableSolutions';

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
    @Inject() public readonly availableSolutions!: () => AvailableSolutions;

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

    public getAvailableSolutions() {
        const activeTask = this.taskTable().getActiveTask();
        if (activeTask) {
            this.taskTable().hide();
            this.availableSolutions().show();
        }
    }

    public getTasks() {
        this.availableSolutions().hide();
        this.taskTable().show();
    }
}
