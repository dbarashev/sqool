import {Contest} from '../Contest';
import {Component, Vue} from 'vue-property-decorator';

@Component
export default class VariantChooser extends Vue {
  private isRandom: boolean = false;
  private isShown: boolean = false;
  private contest?: Contest;
  private onVariantChoice?: (contest: Contest) => void;
  private onFailure?: (xhr: JQuery.jqXHR) => void;

  public show(contest: Contest, onVariantChoice: (contest: Contest) => void, onFailure: (xhr: JQuery.jqXHR) => void) {
    this.isRandom = contest.variantPolicy === 'RANDOM';
    this.isShown = true;
    this.contest = contest;
    this.onVariantChoice = onVariantChoice;
    this.onFailure = onFailure;
  }

  public hide() {
    this.isShown = false;
  }

  public acceptRandom() {
    if (this.contest) {
      this.contest.acceptRandomVariant()
        .done(() => this.onVariantChoice!(this.contest!))
        .fail(this.onFailure!);
    }
  }
}
