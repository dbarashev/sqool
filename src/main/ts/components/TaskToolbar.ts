import {Component, Inject, Vue} from 'vue-property-decorator';
import {TaskDto} from '../Task';
import TaskPropertiesModal from './TaskPropertiesModal';
import TaskMainWindow from './TaskMainWindow';

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
    @Inject() public readonly taskMainWindow!: () => TaskMainWindow;

    public createNewTask() {
        const newTask = new TaskDto(-1, '000', '', '', '');
        this.taskProperties().show(newTask).then((updatedTask) => {
            return $.ajax('/admin/task/new', TaskToolbar.buildTaskPayload(updatedTask));
        }).then(() => {
            this.taskProperties().hide();
            this.taskMainWindow().taskTable().refresh();
        });
    }

    public editTask() {
        const activeTask = this.taskMainWindow().taskTable().getActiveTask();
        if (activeTask) {
            this.taskProperties().show(activeTask).then((updatedTask) => {
                $.ajax('/admin/task/update', TaskToolbar.buildTaskPayload(updatedTask));
            }).then(() => {
                this.taskProperties().hide();
                this.taskMainWindow().taskTable().refresh();
            });
        }
    }

    public buildVariant() {
        this.taskMainWindow().taskTable().buildVariant();
    }

    public showAvailableSolutions() {
        const activeTask = this.taskMainWindow().taskTable().getActiveTask();
        if (activeTask) {
            this.taskMainWindow().showAvailableSolutions();
        }
    }

    public showTasks() {
        this.taskMainWindow().showTaskTable();
    }
}
