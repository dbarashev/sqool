import {Component, Inject, Vue} from 'vue-property-decorator';
import TaskTable from './TaskTable';
import {TaskDto} from '../Task';


@Component
export default class AvailableSolutions extends Vue {
    private solutions = [];
    private taskId = -1;

    @Inject() private readonly taskTable!: () => TaskTable;

    public show() {
        this.refresh();
        $('#available-solutions').show();
    }

    public refresh() {
        this.taskId  = (this.taskTable().getActiveTask() as TaskDto).id;
        $.ajax({
            url: '/admin/submission/get/by/task',
            method: 'GET',
            data: {
                task_id: this.taskId ,
            },
        }).done((solutions: []) => {
            this.solutions = [];
            solutions.forEach((solution) => this.solutions.push(solution));
        });
    }

    public hide() {
        $('#available-solutions').hide();
    }
}


