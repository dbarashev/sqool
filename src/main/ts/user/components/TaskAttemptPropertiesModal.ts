import {Component, Inject, Vue} from 'vue-property-decorator';
import AlertDialog from '../../components/AlertDialog';
import {getTaskSignature, Task, TaskAttempt} from '../TaskAttempt';
import Showdown from 'showdown';

@Component
export default class TaskAttemptPropertiesModal extends Vue {
  @Inject() private readonly alertDialog!: () => AlertDialog;
  private converter = new Showdown.Converter();
  private attempt = new TaskAttempt(new Task(-1, '', null, null, -1, -1), 0, null, null, null);
  private taskSolution = '';
  private taskSignature = '';
  private deferred: JQueryDeferred<string> = $.Deferred<string>();

  public show(attempt: TaskAttempt): JQueryDeferred<string> {
    $('#task-attempt').modal();
    this.attempt = attempt;
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
    this.deferred.resolve(this.taskSolution);
  }
}
