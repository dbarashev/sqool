import {Component, Provide, Vue} from "vue-property-decorator";
import ContestToolbar from "./ContestToolbar";
import ContestTable from "./ContestTable";
import AttemptToolbar from "./AttemptToolbar";
import AttemptTable from "./AttemptTable";
import {ContestDto} from "../Contest";

@Component({
  components: {
    ContestTable, ContestToolbar,
    AttemptToolbar, AttemptTable
  },
})
export default class ContestMainWindow extends Vue {
  private isContestTableVisible = false;
  private isAttemptTableVisible = false;

  public mounted() {
    this.showContestTable();
  }

  public hideChildren() {
    this.isAttemptTableVisible = false;
    this.isContestTableVisible = false;
  }

  public showAttemptTableByTask(contest: ContestDto) {
    this.hideChildren();
    this.isAttemptTableVisible = true;
    this.attemptToolbar().show(contest);
    this.attemptTableByTask().show(contest);
  }

  public showAttemptTableByStudent(contest: ContestDto) {
    this.hideChildren();
    this.isAttemptTableVisible = true;
    this.attemptToolbar().show(contest);
    this.attemptTableByStudent().show(contest);
  }

  public showContestTable() {
    this.hideChildren();
    this.isContestTableVisible = true;
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
