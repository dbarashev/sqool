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
