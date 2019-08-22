import {Component, Vue} from 'vue-property-decorator';
import {ContestDto} from '../Contest';

@Component
export default class AttemptTable extends Vue {
  private contest = new ContestDto('', '', '', '', []);

  public refresh() {

  }

  public hide() {
    this.$el.setAttribute('hidden', 'true');
  }

  public show(contest: ContestDto) {
    this.contest = contest;
    this.refresh();
    this.$el.removeAttribute('hidden');
  }
}
