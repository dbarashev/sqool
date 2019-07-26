import {Component, Inject, Vue} from 'vue-property-decorator';
import {VariantDto} from '../Variant';
import {getTaskResultSql, TaskDto} from '../Task';
import AlertDialog from './AlertDialog';

@Component
export default class VariantPropertiesModal extends Vue {
    public variantName: string = '';
    public tasks: TaskDto[] = [];
    public selectedTasks: TaskDto[] = [];
    private variantId: number = -1;
    private deferred: JQueryDeferred<VariantDto> = $.Deferred<VariantDto>();
    @Inject() private readonly alertDialog!: () => AlertDialog;

    public show(variant: VariantDto): JQueryDeferred<VariantDto> {
        $('#variant-properties').modal();
        this.variantId = variant.id;
        this.variantName = variant.name;
        this.loadTasks(variant.tasks);
        this.deferred = $.Deferred<VariantDto>();
        return this.deferred;
    }

    public taskResultSpec(task: TaskDto): string {
        return getTaskResultSql(task);
    }

    public hide() {
        $('#variant-properties').modal('hide');
    }

    public submit() {
        const selectedTasks = this.selectedTasks.map(task => task.id);
        this.deferred.resolve(new VariantDto(this.variantId, this.variantName, selectedTasks));
    }

    private loadTasks(selectedTaskIdList: number[]) {
        this.tasks = [];
        this.selectedTasks = [];
        $.ajax({
            url: '/admin/task/all',
        }).done((tasks: TaskDto[]) => {
            tasks.forEach(t => {
                this.tasks.push(t);
                if (selectedTaskIdList.includes(t.id)) {
                    this.selectedTasks.push(t);
                }
            });
        }).fail(xhr => {
            const title = 'Не удалось получить список заданий:';
            this.alertDialog().show(title, xhr.statusText);
        });
    }
}
