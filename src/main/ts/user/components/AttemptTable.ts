import {Component, Inject, Vue} from 'vue-property-decorator';
import {TaskAttempt} from '../TaskAttempt';
import AlertDialog from '../../components/AlertDialog';
import TaskAttemptPropertiesModal from './TaskAttemptPropertiesModal';
import FailureDetailsModal from './FailureDetailsModal';
import {Contest} from '../Contest';

@Component
export default class AttemptTable extends Vue {
  @Inject() private readonly alertDialog!: () => AlertDialog;
  @Inject() private readonly taskAttemptProperties!: () => TaskAttemptPropertiesModal;
  @Inject() private readonly failureDetails!: () => FailureDetailsModal;
  // This can't be undefined because it is used in Vue template
  private contest: Contest | null = null;

  public setContest(contest: Contest) {
    this.contest = contest;
  }

  public clear() {
    this.contest = null;
  }

  private getErrorMessage(count: number): string {
    switch (count) {
      case 1:
        return 'Кажется, что-то пошло не так';
      case 2:
        return 'Опять нет';
      case 3:
        return 'Да что ж такое!';
      default:
        return 'Это фиаско, друг!';
    }
  }

  private showTaskAttempt(attempt: TaskAttempt) {
    if (this.contest) {
      const review = $.ajax({
        url: '/review/get',
        data: {
          contest_code: this.contest.contestCode,
          task_id: attempt.taskEntity.id
        },
      });
      this.taskAttemptProperties().show(attempt, review).then((solution) => {
        return $.ajax('/submit.do', this.buildSubmissionPayload(attempt, solution));
      }).done(() => {
        if (this.contest) {
          this.contest.refreshAttempts().fail((xhr) => {
            const title = 'Не удалось обновить вариант:';
            this.alertDialog().show(title, xhr.statusText);
          });
        }
      }).fail((xhr) => {
        const title = 'Не удалось проверить решение:';
        this.alertDialog().show(title, xhr.statusText);
      }).always(() => {
        this.taskAttemptProperties().hide();
      });
    }
  }

  private buildSubmissionPayload(attempt: TaskAttempt, solution: string): object {
    if (this.contest) {
      return {
        method: 'POST',
        data: {
          'task-id': attempt.taskEntity.id,
          'task-name': attempt.taskEntity.name,
          'solution': solution,
          'contest-id': this.contest.contestCode,
          'variant-id': this.contest.chosenVariant ? this.contest.chosenVariant.id : '',
          'variant-name': this.contest.chosenVariant ? this.contest.chosenVariant.name : ''
        },
      };
    } else {
      return {};
    }
  }

  private showFailureDetails(attempt: TaskAttempt) {
    this.failureDetails().show(attempt);
  }
}
