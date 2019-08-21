import {Component, Inject, Vue} from 'vue-property-decorator';
import {ContestDto} from '../Contest';
import AlertDialog from '../../components/AlertDialog';

@Component
export default class AttemptTableByTask extends Vue {
  @Inject() private readonly alertDialog!: () => AlertDialog;
  private contest = new ContestDto('', '', '', '', []);
  private taskStats: TaskAttemptsStat[] = [];

  public mounted() {
    this.refresh();
  }

  public refresh() {
    $.ajax({
      url: '/admin/submission/contest/stats',
      data: {contest_code: this.contest.code}
    }).done((taskStats: TaskAttemptsStat[]) => {
      this.taskStats = taskStats;
    }).fail((xhr) => {
      const title = 'Не удалось загрузить попытки:';
      this.alertDialog().show(title, xhr.statusText);
    });
  }

  public hide() {
    this.$el.setAttribute('hidden', 'true');
  }

  public show(contest: ContestDto) {
    this.contest = contest;
    this.refresh();
    this.$el.removeAttribute('hidden');
  }
}

class TaskAttemptsStat {
  constructor(
      readonly task_id: number,
      readonly task_name: string,
      readonly solved: number,
      readonly attempted: number
  ) {}
}
