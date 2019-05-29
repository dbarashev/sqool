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
        this.taskId  = (this.taskMainWindow().taskTable().getActiveTask() as TaskDto).id;
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

    public getReviewPage(userId: number) {
       this.taskMainWindow().reviewPage().userId = userId;
       this.taskMainWindow().reviewPage().taskId = this.taskId;
       this.taskMainWindow().showReviewPage();
    }
}


