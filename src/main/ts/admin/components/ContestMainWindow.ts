import {Component, Provide, Vue} from 'vue-property-decorator';
import ContestToolbar from './ContestToolbar';
import ContestTable from './ContestTable';
import AttemptToolbar from './AttemptToolbar';
import AttemptTable from './AttemptTable';
import {ContestDto} from '../Contest';

type VisibleComponent = 'contests' | 'attempts-by-task' | 'attempts-by-student';
@Component({
  components: {
    ContestTable, ContestToolbar,
    AttemptToolbar, AttemptTable,
  },
})
export default class ContestMainWindow extends Vue {
  private visibleComponent: VisibleComponent = 'contests';
  public mounted() {
    this.showContestTable();
  }


  public showAttemptTableByTask(contest: ContestDto) {
    this.visibleComponent = 'attempts-by-task';
    this.attemptToolbar().show(contest);
    this.attemptTableByTask().show(contest);
  }

  public showAttemptTableByStudent(contest: ContestDto) {
    this.visibleComponent = 'attempts-by-student';
    this.attemptToolbar().show(contest);
    this.attemptTableByStudent().show(contest);
  }

  public showContestTable() {
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
  public attemptTableByTask(): AttemptTable {
    return this.$refs.attemptTableByTask as AttemptTable;
  }
}
