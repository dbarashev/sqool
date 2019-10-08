import {Component, Provide, Vue} from 'vue-property-decorator';
import ContestToolbar from './ContestToolbar';
import ContestTable from './ContestTable';
import AttemptToolbar from './AttemptToolbar';
import AttemptTableByTask from './AttemptTableByTask';
import AttemptTableByStudent from './AttemptTableByStudent';
import {ContestDto} from '../Contest';
import ReviewPage from './ReviewPage';
import {Attempt} from './AttemptTable';

type VisibleComponent = 'contests' | 'attempts-by-task' | 'attempts-by-student';
@Component({
  components: {
    ContestTable, ContestToolbar, ReviewPage,
    AttemptToolbar, AttemptTableByTask, AttemptTableByStudent,
  },
})
export default class ContestMainWindow extends Vue {
  private visibleComponent: VisibleComponent = 'contests';
  private contest?: ContestDto;

  public mounted() {
    this.showContestTable();
  }

  public hideChildren() {
    this.attemptTableByTask().hide();
    this.attemptTableByStudent().hide();
    this.reviewPage().hide();
  }

  public showAttemptTableByTask(contest: ContestDto) {
    this.hideChildren();
    this.visibleComponent = 'attempts-by-task';
    this.$nextTick(() => {
      this.attemptToolbar().show(contest);
      this.attemptTableByTask().show(contest);
    });
  }

  public showAttemptTableByStudent(contest: ContestDto) {
    this.hideChildren();
    this.visibleComponent = 'attempts-by-student';
    this.contest = contest;
    this.$nextTick(() => {
      this.attemptToolbar().show(contest);
      this.attemptTableByStudent().show(contest);
    });
  }

  public showContestTable() {
    this.hideChildren();
    this.visibleComponent = 'contests';
  }

  public showReviewPage(attempt: Attempt) {
    this.hideChildren();
    this.attemptToolbar().hide();
    this.reviewPage().show(this.contest!!, attempt);
  }

  @Provide()
  public contestToolbar(): ContestToolbar {
    return this.$refs.contestToolbar as ContestToolbar;
  }

  @Provide()
  public contestTable(): ContestTable {
    return this.$refs.contestTable as ContestTable;
  }

  @Provide()
  public attemptToolbar(): AttemptToolbar {
    return this.$refs.attemptToolbar as AttemptToolbar;
  }

  @Provide()
  public attemptTableByStudent(): AttemptTableByStudent {
    return this.$refs.attemptTableByStudent as AttemptTableByStudent;
  }

  @Provide()
  public attemptTableByTask(): AttemptTableByTask {
    return this.$refs.attemptTableByTask as AttemptTableByTask;
  }

  @Provide()
  public reviewPage(): ReviewPage {
    return this.$refs.reviewPage as ReviewPage;
  }
}
