import {Component, Inject, Prop, Vue} from 'vue-property-decorator';
import AlertDialog from '../../components/AlertDialog';
import {Attempt} from '../Attempt';
import ContestMainWindow from "./ContestMainWindow";

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
        user_id: this.userId
      }
    }).done((attempts: Attempt[]) => {
      this.attempts = attempts;
    }).fail((xhr) => {
      const title = 'Не удалось загрузить попытки:';
      this.alertDialog().show(title, xhr.statusText);
    });
  }

  public showReviewPage(attempt: Attempt) {
    if (attempt.attempt_id) {
      this.contestMainWindow().showReviewPage(attempt.attempt_id);
    } else {
      const title = 'По этой задаче нет попыток';
      this.alertDialog().show(title);
    }
  }
}