import {TaskAttempt} from './TaskAttempt';
import {Inject} from 'vue-property-decorator';
import AlertDialog from '../components/AlertDialog';

export type VariantPolicy = 'ANY' | 'RANDOM' | 'ALL';

export class Contest {
  public attempts: TaskAttempt[] = [];
  @Inject() private readonly alertDialog!: () => AlertDialog;

  constructor(readonly contestCode: string, readonly variantPolicy: VariantPolicy) {}

  public refreshAttempts() {
    return $.ajax({
      url: '/contest/attempts',
      data: {contest_code: this.contestCode},
    }).done((attempts: TaskAttempt[]) => {
      this.attempts = attempts;
      const hasTesting = this.attempts.some((attempt) => attempt.status === 'testing');
      if (hasTesting) {
        window.setTimeout(() => this.refreshAttempts(), 1000);
      }
    });
  }
}
