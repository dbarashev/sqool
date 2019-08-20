import {ContestDto} from '../Contest';
import {Component, Inject, Vue} from 'vue-property-decorator';
import AlertDialog from '../../components/AlertDialog';
import ContestToolbar from './ContestToolbar';
import TaskAttempts from './AttemptsModal.vue';
import AttemptsModal from './AttemptsModal';
import ContestMainWindow from "./ContestMainWindow";

@Component({
  components: {
    TaskAttempts,
  },
})
export default class ContestTable extends Vue {
  public contests: ContestDto[] = [];
  private activeContest?: ContestDto;
  @Inject() private readonly alertDialog!: () => AlertDialog;
  @Inject() private readonly contestToolbar!: () => ContestToolbar;
  @Inject() private readonly attemptsModal!: () => AttemptsModal;
  @Inject() private readonly contestMainWindow!: () => ContestMainWindow;


  public mounted() {
    this.refresh();
  }

  public refresh() {
    $.ajax({
      url: '/admin/contest/all',
    }).done((contests: ContestDto[]) => {
      this.contests = [];
      contests.forEach((c) => this.contests.push(c));
    }).fail((xhr) => {
      const title = 'Не удалось получить список контестов:';
      this.alertDialog().show(title, xhr.statusText);
    });
  }

  public getActiveContest(): ContestDto | undefined {
    return this.activeContest;
  }

  public makeActive(contest: ContestDto) {
    contest.active = true;
    if (this.activeContest) {
      this.activeContest.active = false;
    }
    this.activeContest = contest;
    this.$forceUpdate();
  }

  public editContest() {
    this.contestToolbar().editContest();
  }

  public showAttemptsModal(contest: ContestDto) {
    this.attemptsModal().show(contest);
  }

  public showAttemptsTable(contest: ContestDto) {
    this.contestMainWindow().showAttemptTableByTask(contest);
  }

  public hide() {
    this.$el.setAttribute('hidden', 'true');
  }

  public show() {
    this.refresh();
    this.$el.removeAttribute('hidden');
  }
}
