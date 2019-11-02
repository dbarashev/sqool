import {Component, Inject, Prop, Vue} from 'vue-property-decorator';
import AlertDialog from '../../components/AlertDialog';
import ContestMainWindow from './ContestMainWindow';

export interface Attempt {
  task_id: number;
  variant_id: number;
  name: string;
  attempt_id: string | null;
  status: string;
  user_name: string;
  count: number;
  testing_start_ts: number;
}

@Component
export default class AttemptTable extends Vue {
  @Inject() private readonly alertDialog!: () => AlertDialog;
  @Inject() private readonly contestMainWindow!: () => ContestMainWindow;
  @Prop() private readonly contestCode!: string;
  @Prop() private readonly userId!: number;
  private attempts: Attempt[] = [];

  public refresh() {
    $.ajax({
      url: '/admin/submission/contest',
      data: {
        contest_code: this.contestCode,
        user_id: this.userId,
      },
    }).done((attempts: Attempt[]) => {
      this.attempts = attempts;
    }).fail((xhr) => {
      const title = 'Не удалось загрузить попытки:';
      this.alertDialog().show(title, xhr.statusText);
    });
  }

  public showReviewPage(attempt: Attempt) {
    if (attempt.attempt_id) {
      this.contestMainWindow().showReviewPage(attempt);
    } else {
      const title = 'По этой задаче нет попыток';
      this.alertDialog().show(title);
    }
  }

  public formatStatus(attempt: Attempt): string {
    switch (attempt.status) {
      case 'failure': return 'Решена неверно';
      case 'virgin': return 'Не решалась';
      case 'success': return 'Решена успешно';
      case 'testing': return 'Тестируется';
    }
    return '';
  }

  public formatTimestamp(attempt: Attempt): string {
    return new Date(attempt.testing_start_ts).toLocaleString('ru-RU', {
      year: 'numeric',
      month: 'short',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
    });
  }

}
