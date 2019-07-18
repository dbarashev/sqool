import {Component, Inject, Vue} from 'vue-property-decorator';
import {TaskDto} from '../Task';
import TaskPropertiesModal from './TaskPropertiesModal';
import TaskMainWindow from './TaskMainWindow';
import AlertDialog from './AlertDialog';

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
    @Inject() private readonly alertDialog!: () => AlertDialog;

    public createNewTask() {
        const newTask = new TaskDto(-1, '000', '', '', '');
        this.showAndSubmitTask(newTask, '/admin/task/new');
    }

    public editTask() {
        const activeTask = this.taskMainWindow().taskTable().getActiveTask();
        if (activeTask) {
            this.showAndSubmitTask(activeTask, '/admin/task/edit');
        }
    }

    private showAndSubmitTask(task: TaskDto, url: string) {
        this.taskProperties().show(task).then((updatedTask) => {
            task = updatedTask;
            return $.ajax(url, TaskToolbar.buildTaskPayload(updatedTask))
        }).done(() => {
            this.taskProperties().hide();
            this.taskMainWindow().taskTable().refresh();
        }).fail(xhr => {
            this.showAndSubmitTask(task, url);
            const title = `Что-то пошло не так: ${xhr.status}`;
            this.alertDialog().show(title, xhr.statusText);
        });
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
