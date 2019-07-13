import {Component, Inject, Vue} from 'vue-property-decorator';
import {getTaskResultSql, TaskDto} from '../Task';
import VariantBuildingProgressBar from './VariantBuildingProgressBar';
import AlertDialog from './AlertDialog';
import TaskMainWindow from './TaskMainWindow';

@Component
export default class TaskTable extends Vue {
    public tasks: TaskDto[] = [];
    public selectedTasks: TaskDto[] = [];
    private activeTask?: TaskDto;
    @Inject()
    private readonly variantBuildingProgressBar!: () => VariantBuildingProgressBar;
    @Inject()
    private readonly alertDialog!: () => AlertDialog;


    public mounted() {
        this.refresh();
    }

    public taskResultSpec(task: TaskDto): string {
        return getTaskResultSql(task);
    }

    public buildVariant() {
        this.variantBuildingProgressBar().show();
        const taskIdList = this.selectedTasks.map((task) => task.id);
        $.post('/admin/variant/new', {
            course: 'course',
            module: 'module',
            variant: 'variant',
            schema: 'schema',
            tasks: JSON.stringify(taskIdList),
        }).done((result: ImageBuildingResult) => {
            let title = '';
            if (result.result === 'OK') {
                title = 'Вариант успешно создан';
            } else {
                title = 'В имени/решении/спецификации задач найдены синтаксические ошибки:';
            }
            this.alertDialog().show(title, result.message);
        }).fail((xhr) => {
            let title = '';
            if (xhr.status >= 500 && xhr.status < 600) {
                title = 'При создании варианта произошла внутренняя ошибка сервера';
            } else {
                title = `Что-то пошло не так: ${xhr.status}`;
            }
            this.alertDialog().show(title);
        }).always(() => {
            this.variantBuildingProgressBar().hide();
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

    public hide() {
        this.$el.setAttribute('hidden', 'true');
    }

    public show() {
        this.refresh();
        this.$el.removeAttribute('hidden');
    }
}

class ImageBuildingResult {
    constructor(readonly result: string, readonly message: string = '') {}
}