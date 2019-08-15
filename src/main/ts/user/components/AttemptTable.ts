import {Component, Vue} from 'vue-property-decorator';
import {TaskAttempt} from '../TaskAttempt';

@Component
export default class AttemptTable extends Vue {
  private attempts: TaskAttempt[] = [];

  public setAttempts(attempts: TaskAttempt[]) {
    this.attempts = [];
    attempts.forEach((a) => this.attempts.push(a));
  }

  private getErrorMessage(count: number): string {
    switch (count) {
      case 1:
        return 'Кажется, что-то пошло не так';
      case 2:
        return 'Опять нет';
      case 3:
        return 'Да что ж такое!';
      default:
        return 'Это фиаско, друг!';
    }
  }

  clear() {
    this.attempts = [];
  }
}
