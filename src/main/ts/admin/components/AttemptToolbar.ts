import {Component, Inject, Vue} from 'vue-property-decorator';
import ContestMainWindow from './ContestMainWindow';
import {ContestDto} from '../Contest';
import {Student} from './AttemptTableByStudent';

type ActiveView = 'by-tasks' | 'by-students';

@Component
export default class AttemptToolbar extends Vue {
  private contest = new ContestDto('', '', '', '', []);
  @Inject() private readonly contestMainWindow!: () => ContestMainWindow;
  private activeView: ActiveView = 'by-students';
  private selection: Student[] = [];

  public showContests() {
    this.contestMainWindow().showContestTable();
  }

  public showAttemptsByStudents() {
    this.contestMainWindow().showAttemptTableByStudent(this.contest);
    this.activeView = 'by-students';
  }

  public showAttemptsByTasks() {
    this.contestMainWindow().showAttemptTableByTask(this.contest);
    this.activeView = 'by-tasks';
  }

  public show(contest: ContestDto) {
    $(this.$el).removeClass('d-none').addClass('d-flex');
    this.contest = contest;
  }

  public hide() {
    $(this.$el).removeClass('d-flex').addClass('d-none');
  }

  public emailReviews() {
    this.selection.forEach((student) => {
      this.emailStudentReviews(student.user_id, this.contest.code);
    });
  }

  public getSelectionListener(): (selected: Student[]) => void {
    const self = this;
    return (selection: Student[]) => {
      self.selection = selection;
    };
  }

  private emailStudentReviews(userId: number, contestCode: string) {
    $.ajax({
      method: 'POST',
      url: '/admin/review/email',
      data: {contest_code: contestCode, user_id: userId},
    }).fail((xhr) => {
      this.showError('Что-то пошло не так во время рассылки', xhr.statusText);
    });
  }

  private showError(title: string, text: string) {
    this.contestMainWindow().attemptTableByStudent().alertDialog().show(title, text);
  }
}
