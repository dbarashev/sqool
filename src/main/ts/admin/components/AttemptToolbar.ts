import {Component, Inject, Vue} from 'vue-property-decorator';
import ContestMainWindow from './ContestMainWindow';
import {ContestDto} from '../Contest';

type ActiveView = 'by-tasks' | 'by-students';

@Component
export default class AttemptToolbar extends Vue {
  private contest = new ContestDto('', '', '', '', []);
  @Inject() private readonly contestMainWindow!: () => ContestMainWindow;
  private activeView: ActiveView = 'by-students';

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
}
