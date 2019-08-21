import {Component, Inject, Vue} from 'vue-property-decorator';
import {ContestDto} from '../Contest';
import AlertDialog from '../../components/AlertDialog';
import {Attempt} from '../Attempt';

@Component
export default class AttemptTableByTask extends Vue {
  @Inject() private readonly alertDialog!: () => AlertDialog;
  private contest = new ContestDto('', '', '', '', []);
  private attempts = new Map<string, Attempt[]>();

  public mounted() {
    this.refresh();
  }

  public refresh() {
    $.ajax({
      url: '/admin/submission/contest',
      data: {contest_code: this.contest.code}
    }).done((attempts: Attempt[]) => {
      this.attempts = new Map<string, Attempt[]>();
      attempts.forEach(attempt => {
        this.attempts.set(attempt.name, this.attempts.get(attempt.name) || []);
        this.attempts.get(attempt.name)!.push(attempt);
      })
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
