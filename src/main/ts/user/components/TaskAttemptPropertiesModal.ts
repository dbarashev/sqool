import {Component, Inject, Vue} from 'vue-property-decorator';
import AlertDialog from '../../components/AlertDialog';
import {getTaskSignature, Task, TaskAttempt} from '../TaskAttempt';
import Showdown from 'showdown';
import FailureDetails from './FailureDetails';

@Component({
  components: {
    FailureDetails,
  },
})
export default class TaskAttemptPropertiesModal extends Vue {
  @Inject() private readonly alertDialog!: () => AlertDialog;

  private converter = new Showdown.Converter();
  private attempt = new TaskAttempt(null, new Task(-1, '', null, null, -1, -1, -1), 0, null, null, null);
  private review = '';
  private taskSolution = '';
  private taskSignature = '';
  private hasSchema = false;
  private schemaBodyUrl = '';
  private isGrading = false;
  private deferred: JQueryDeferred<string> = $.Deferred<string>();

  public show(attempt: TaskAttempt, ajaxReview: JQuery.jqXHR, onSubmit: (solution: string) => void) {
    $('#task-attempt').modal();
    this.attempt = attempt;
    this.hasSchema = attempt.taskEntity.schemaId != null;
    this.schemaBodyUrl = this.hasSchema ? `/script/body?id=${attempt.taskEntity.schemaId}` : '';
    this.review = '';
    this.handleReview(ajaxReview);
    this.taskSignature = getTaskSignature(attempt.taskEntity);
    this.taskSolution = localStorage.getItem(String(attempt.taskEntity.id)) || '';
    this.failureDetails().show(attempt);
    this.onSubmit = onSubmit;
  }

  public hide() {
    $('#task-attempt').modal('hide');
  }

  public markdownText(text: string): string {
    return this.converter.makeHtml(text);
  }

  public processAttempt(attempts: TaskAttempt[]) {
    this.isGrading = false;
    const attempt = attempts.find((attempt) => attempt.taskEntity.id === this.attempt.taskEntity.id);
    if (attempt) {
      if (attempt.status === 'success') {
        this.hide();
      } else {
        this.failureDetails().show(attempt);
        $(this.$el).find('#review-tab').trigger('click');
      }
    }
  }
  private onSubmit: (solution: string) => void = () => {};
  private failureDetails(): FailureDetails {
    return this.$refs.failureDetails as FailureDetails;
  }

  private submit() {
    if (this.taskSolution === '') {
      this.alertDialog().show('Решение не может быть пустым');
      return;
    }
    localStorage.setItem(String(this.attempt.taskEntity.id), this.taskSolution);
    this.isGrading = true;
    this.onSubmit(this.taskSolution);
  }

  private handleReview(ajaxReview: JQuery.jqXHR) {
    ajaxReview.done((review) => {
      this.review = review;
    }).fail((xhr) => {
      const title = 'Не удалось загрузить ревью:';
      this.alertDialog().show(title, xhr.statusText);
    });
  }
}
