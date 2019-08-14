import {TaskAttempt} from './TaskAttempt';
import {Inject} from 'vue-property-decorator';
import AlertDialog from '../components/AlertDialog';

export type VariantPolicy = 'ANY' | 'RANDOM' | 'ALL';

export class VariantOption {
  constructor(readonly id: number, readonly name: string) {
  }
}

export class Contest {
  public attempts: TaskAttempt[] = [];
  @Inject() private readonly alertDialog!: () => AlertDialog;

  constructor(readonly contestCode: string,
              readonly variantPolicy: VariantPolicy,
              readonly variants: VariantOption[]) {}

  public refreshAttempts(): JQuery.jqXHR {
    return $.ajax({
      url: '/contest/attempts',
      data: {contest_code: this.contestCode},
    }).done((attempts: TaskAttempt[]) => {
      this.attempts = attempts;
    });
  }

  public acceptRandomVariant(): JQuery.jqXHR {
    return $.ajax({
      url: '/acceptContest',
      method: 'POST',
      data: {contest_code: this.contestCode}
    });

  }
}
