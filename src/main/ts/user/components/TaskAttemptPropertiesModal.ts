import {Component, Inject, Vue} from 'vue-property-decorator';
import AlertDialog from '../../components/AlertDialog';
import {getTaskSignature, Task, TaskAttempt} from '../TaskAttempt';
import Showdown from 'showdown';

@Component
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

  public show(attempt: TaskAttempt, ajaxReview: JQuery.jqXHR): JQueryDeferred<string> {
    $('#task-attempt').modal();
    this.attempt = attempt;
    this.hasSchema = attempt.taskEntity.schemaId != null;
    this.schemaBodyUrl = this.hasSchema ? `/script/body?id=${attempt.taskEntity.schemaId}` : '';
    this.review = '';
    this.handleReview(ajaxReview);
    this.taskSignature = getTaskSignature(attempt.taskEntity);
    this.taskSolution = localStorage.getItem(String(attempt.taskEntity.id)) || '';
    this.deferred = $.Deferred<string>();
    return this.deferred;
  }

  public hide() {
    $('#task-attempt').modal('hide');
  }

  public markdownText(text: string): string {
    return this.converter.makeHtml(text);
  }

  private submit() {
    if (this.taskSolution === '') {
      this.alertDialog().show('Решение не может быть пустым');
      return;
    }
    localStorage.setItem(String(this.attempt.taskEntity.id), this.taskSolution);
    this.isGrading = true;
    this.deferred.resolve(this.taskSolution);
  }

  private handleReview(ajaxReview: JQuery.jqXHR) {
    ajaxReview.done(review => {
      this.review = review;
    }).fail((xhr) => {
      const title = 'Не удалось загрузить ревью:';
      this.alertDialog().show(title, xhr.statusText);
    })
  }

  processAttempt(attempts: TaskAttempt[]) {
    this.isGrading = false;
    let attempt = attempts.find((attempt) => attempt.taskEntity.id === this.attempt.taskEntity.id);
    if (attempt) {
      if (attempt.status === 'success') {
        this.hide();
      } else {
        this.review = `
### Что-то пошло не так

${attempt.errorMsg}
`;
        $(this.$el).find("#review-tab").trigger("click");
      }
    }
  }
}
