import {Component, Provide, Vue} from 'vue-property-decorator';
import ContestToolbar from './ContestToolbar';
import ContestTable from './ContestTable';
import AttemptToolbar from './AttemptToolbar';
import AttemptTableByTask from './AttemptTableByTask';
import AttemptTable from './AttemptTable';
import {ContestDto} from '../Contest';

type VisibleComponent = 'contests' | 'attempts-by-task' | 'attempts-by-student';
@Component({
  components: {
    ContestTable, ContestToolbar,
    AttemptToolbar, AttemptTableByTask, AttemptTable,
  },
})
export default class ContestMainWindow extends Vue {
  private visibleComponent: VisibleComponent = 'contests';

  public mounted() {
    this.showContestTable();
  }

  public hideAttemptTables() {
    this.attemptTableByTask().hide();
    this.attemptTableByStudent().hide();
  }

  public showAttemptTableByTask(contest: ContestDto) {
    this.hideAttemptTables();
    this.visibleComponent = 'attempts-by-task';
    this.$nextTick(() => {
      this.attemptToolbar().show(contest);
      this.attemptTableByTask().show(contest);
    });
  }

  public showAttemptTableByStudent(contest: ContestDto) {
    this.hideAttemptTables();
    this.visibleComponent = 'attempts-by-student';
    this.$nextTick(() => {
      this.attemptToolbar().show(contest);
      this.attemptTableByStudent().show(contest);
    });
  }

  public showContestTable() {
    this.hideAttemptTables();
    this.visibleComponent = 'contests';
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
  public attemptTableByStudent(): AttemptTable {
    return this.$refs.attemptTableByStudent as AttemptTable;
  }

  @Provide()
  public attemptTableByTask(): AttemptTableByTask {
    return this.$refs.attemptTableByTask as AttemptTableByTask;
  }
}
