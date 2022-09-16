import {Contest, VariantOption} from '../Contest';
import {Component, Vue} from 'vue-property-decorator';

@Component
export default class VariantChooser extends Vue {
  private isRandom: boolean = false;
  private isShown: boolean = false;
  private contest?: Contest;
  private onVariantChoice?: (contest: Contest) => void;
  private onFailure?: (xhr: JQuery.jqXHR) => void;
  private variants: VariantOption[] = [];
  private selectedVariantName: string = 'Выберите вариант';
  private selectedVariantOption: VariantOption | null = null;

  public show(contest: Contest, onVariantChoice: (contest: Contest) => void, onFailure: (xhr: JQuery.jqXHR) => void) {
    this.isRandom = contest.variantPolicy === 'RANDOM';
    this.isShown = true;
    this.contest = contest;
    this.variants = this.contest.variants;
    this.onVariantChoice = onVariantChoice;
    this.onFailure = onFailure;
  }

  public hide() {
    this.isShown = false;
  }

  public selectVariant(v: VariantOption) {
    this.selectedVariantOption = v;
    this.selectedVariantName = v.name;
  }

  public acceptRandom() {
    if (this.contest) {
      this.contest.acceptRandomVariant()
        .done(() => this.onVariantChoice!(this.contest!))
        .fail(this.onFailure!);
    }
  }

  public acceptSelected() {
    if (this.contest && this.selectedVariantOption) {
      this.contest.acceptVariant(this.selectedVariantOption)
          .done(() => this.onVariantChoice!(this.contest!))
          .fail(this.onFailure!);
    }
  }
}
