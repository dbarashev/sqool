import {Contest} from '../Contest';
import {Component, Vue} from 'vue-property-decorator';

@Component
export default class VariantChooser extends Vue {
  private isRandom: boolean = false;
  private isShown: boolean = false;
  public show(contest: Contest) {
    this.isRandom = contest.variantPolicy === 'RANDOM';
    this.isShown = true;
  }

  public hide() {
    this.isShown = false;
  }
}
