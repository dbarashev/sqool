import {Component, Inject, Vue} from 'vue-property-decorator';
import {ContestDto} from '../Contest';
import AlertDialog from '../../components/AlertDialog';
import AttemptTable from './AttemptTable';

@Component({
  components: {AttemptTable},
})
export default class AttemptTableByStudent extends Vue {
  @Inject() private readonly alertDialog!: () => AlertDialog;
  private contest = new ContestDto('', '', '', '', []);
  private students: Student[] = [];
  private emailProgress: boolean = false;

  public mounted() {
    this.refresh();
  }

  public refresh() {
    $.ajax({
      url: '/admin/submission/contest/users',
      data: {contest_code: this.contest.code},
    }).done((students: Student[]) => {
      this.students = students;
    }).fail((xhr) => {
      const title = 'Не удалось получить список студентов:';
      this.alertDialog().show(title, xhr.statusText);
    });
  }

  public refreshUserTable(ref: string) {
    this.userAttemptTable(ref).refresh();
  }

  public hide() {
    this.$el.setAttribute('hidden', 'true');
  }

  public show(contest: ContestDto) {
    this.contest = contest;
    this.refresh();
    this.$el.removeAttribute('hidden');
  }

  public emailReviews(userId: number, contestCode: string) {
    this.emailProgress = true;
    $.ajax({
      method: 'POST',
      url: '/admin/review/email',
      data: {contest_code: contestCode, user_id: userId},
    }).fail((xhr) => {
      this.alertDialog().show('Что-то пошло не так во время рассылки', xhr.statusText);
    }).always( () => {
      this.emailProgress = false;
    });
  }

  private userAttemptTable(ref: string): AttemptTable {
    const table = this.$refs[ref] as AttemptTable[];
    return table[0];
  }

}

interface Student {
  user_id: number;
  user_name: string;
  uni: string;
}
