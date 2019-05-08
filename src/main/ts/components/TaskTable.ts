import {Component, Inject, Vue} from 'vue-property-decorator';
import {ColumnSpec, getTaskResultSql, TaskDto} from '../Task';
import VariantBuildingProgressBar from "./VariantBuildingProgressBar.vue";
import AlertDialog from "./AlertDialog.vue";

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
        const jsonTasks = this.selectedTasks.map((task) => ({
                name: task.name,
                keyAttributes: JSON.parse(task.result_json),
                nonKeyAttributes: [],
                solution: 'Put teacher\'s query here',
            }),
        );

        this.variantBuildingProgressBar().show();
        $.post('/admin/variant/new', {
            course: 'course',
            module: 'module',
            variant: 'variant',
            schema: 'schema',
            tasks: JSON.stringify(jsonTasks),
        }).always((xhr) => {
            let title = "";
            let message = "";
            if (xhr.status >= 200 && xhr.status < 300) {
                title = "Вариант успешно создан"
            } else if (xhr.status >= 400 && xhr.status < 500) {
                title = "В имени/решении/спецификации задач найдены синтаксичесие ошибки:"
                message = $(xhr.responseText).filter('title').text();
            } else if (xhr.status >= 500 && xhr.status < 600) {
                title = "При создании варианта произошла внутрення ошибка сервера"
            }

            this.variantBuildingProgressBar().hide();
            this.alertDialog().show(title, message)
        })
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
