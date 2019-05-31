import {Component, Inject, Vue} from 'vue-property-decorator';
import {TaskDto} from '../Task';
import TaskMainWindow from './TaskMainWindow';


@Component
export default class AvailableSolutions extends Vue {
    private solutions = [];
    private taskId = -1;

    @Inject()
    private readonly taskMainWindow!: () => TaskMainWindow;

    public refresh() {
        if (this.taskMainWindow().taskTable().getActiveTask() == null) {
           return ;
        }
        this.taskId  = (this.taskMainWindow().taskTable().getActiveTask() as TaskDto).id;
        $.ajax({
            url: '/admin/submission/list',
            method: 'GET',
            data: {
                task_id: this.taskId ,
            },
        }).done((solutions: []) => {
            this.solutions = [];
            solutions.forEach((solution) => this.solutions.push(solution));
        });
    }

    public showReviewPage(userId: number) {
       this.taskMainWindow().showReviewPage(userId, this.taskId);
    }

    public hide() {
        $('#available-solutions').hide();
    }

    public show() {
        this.refresh();
        $('#available-solutions').show();
    }
}


