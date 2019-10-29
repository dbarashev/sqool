import {TaskAttempt} from './TaskAttempt';

export type VariantPolicy = 'ANY' | 'RANDOM' | 'ALL';

export class VariantOption {
  constructor(readonly id: number, readonly name: string) {
  }
}

export class Contest {
  public attempts: TaskAttempt[] = [];

  constructor(readonly contestCode: string,
              readonly variantPolicy: VariantPolicy,
              readonly variants: VariantOption[],
              public chosenVariant?: VariantOption) {}

  public refreshAttempts(): JQuery.jqXHR {
    return $.ajax({
      url: '/contest/attempts',
      data: {contest_code: this.contestCode},
    }).done((attempts: TaskAttempt[]) => {
      this.attempts = attempts;
    });
  }

  public acceptRandomVariant(): JQuery.jqXHR {
    const result = $.ajax({
      url: '/contest/accept',
      method: 'POST',
      data: {contest_code: this.contestCode},
    });
    return result.done((variant: VariantOption) => {
      this.chosenVariant = variant;
    });
  }

  public acceptVariant(variant: VariantOption): JQuery.jqXHR {
    const result = $.ajax({
      url: '/contest/accept',
      method: 'POST',
      data: {
        contest_code: this.contestCode,
        variant_id: variant.id,
      },
    });
    return result.done(() => {
      this.chosenVariant = variant;
    });
  }
}
