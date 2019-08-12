import {TaskAttempt} from "./TaskAttempt";
import {Inject} from 'vue-property-decorator';
import AlertDialog from '../components/AlertDialog';

export class Challenge {
  @Inject() private readonly alertDialog!: () => AlertDialog;
  public attempts: TaskAttempt[] = [];
  public contestCode = '';

  public refreshAttempts() {}

  protected handleAttempts(attempts: TaskAttempt[]) {
    this.attempts = attempts;
    const hasTesting = this.attempts.some(attempt => attempt.status === 'testing');
    if (hasTesting) {
      window.setTimeout(() => this.refreshAttempts(), 1000);
    }
  }
}

export class ContestChallenge extends Challenge {
  constructor(public contestCode: string) {
    super()
  }

  public refreshAttempts() {
    return $.ajax({
      url: '/contest/attempts',
      data: {code: this.contestCode}
    }).done((tasks: TaskAttempt[]) => this.handleAttempts(tasks))
  }
}

export class VariantChallenge extends Challenge {
  constructor(public contestCode: string, private variantId: number) {
    super()
  }

  public refreshAttempts() {
    return $.ajax({
      url: '/variant/attempts',
      data: {id: this.variantId}
    }).done((tasks: TaskAttempt[]) => this.handleAttempts(tasks))
  }
}