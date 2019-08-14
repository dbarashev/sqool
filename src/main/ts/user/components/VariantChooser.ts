import {Contest, VariantOption} from '../Contest';
import {Component, Vue} from 'vue-property-decorator';

@Component
export default class VariantChooser extends Vue {
  private isRandom: boolean = false;
  private isShown: boolean = false;
  private contest?: Contest;
  private onVariantChoice?: (contest: Contest) => void;
  private onFailure?: (xhr: JQuery.jqXHR) => void;
  private selectedVariantName: string = "Выберите вариант";

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

  variants(): VariantOption[] {
    return (this.contest) ? this.contest.variants : [];
  }

  selectVariant(v: VariantOption) {
    this.selectedVariantName = v.name;
  }

  acceptRandom() {
    if (this.contest) {
      this.contest.acceptRandomVariant()
        .done(() => this.onVariantChoice!(this.contest!))
        .fail(this.onFailure!);
    }
  }

  acceptSelected() {

  }
}
