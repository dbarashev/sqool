import {Component, Inject, Vue} from "vue-property-decorator";
import ContestMainWindow from "./ContestMainWindow";
import {ContestDto} from "../Contest";

@Component
export default class AttemptToolbar extends Vue {
  private contest = new ContestDto("", "", "", "", []);
  @Inject() private readonly contestMainWindow!: () => ContestMainWindow;

  public showContests() {
    this.contestMainWindow().showContestTable();
  }

  public showAttemptsByStudents() {
    this.contestMainWindow().showAttemptTableByStudent(this.contest);
  }

  public showAttemptsByTasks() {
    this.contestMainWindow().showAttemptTableByTask(this.contest);
  }

  public hide() {
    $('#attempt-toolbar').attr('style', 'display: none !important');
  }

  public show(contest: ContestDto) {
    this.contest = contest;
    $('#attempt-toolbar').attr('style', 'display: block');
  }
}