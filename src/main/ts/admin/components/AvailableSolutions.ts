import {Component, Inject, Vue} from 'vue-property-decorator';
import {TaskDto} from '../Task';
import TaskMainWindow from './TaskMainWindow';
import AlertDialog from "../../components/AlertDialog";


@Component
export default class AvailableSolutions extends Vue {
  private solutions = [];
  private taskId = -1;

  @Inject()
  private readonly taskMainWindow!: () => TaskMainWindow;
  @Inject() private readonly alertDialog!: () => AlertDialog;

  public refresh() {
    if (this.taskMainWindow().taskTable().getActiveTask() == null) {
      return;
    }
    this.taskId = (this.taskMainWindow().taskTable().getActiveTask() as TaskDto).id;
    $.ajax({
      url: '/admin/submission/list',
      method: 'GET',
      data: {
        task_id: this.taskId,
      },
    }).done((solutions: []) => {
      this.solutions = [];
      solutions.forEach((solution) => this.solutions.push(solution));
    });
  }

  public showReviewPage(attemptId: string | null) {
    if (attemptId) {
      this.taskMainWindow().showReviewPage(attemptId);
    } else {
      const title = 'По этой задаче нет попыток';
      this.alertDialog().show(title);
    }
  }

  public hide() {
    this.$el.setAttribute('hidden', 'true');
  }

  public show() {
    this.refresh();
    this.$el.removeAttribute('hidden');
  }
}


