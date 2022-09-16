import {Component, Inject, Vue} from 'vue-property-decorator';
import FailureDetails from './FailureDetails';
import {Task, TaskAttempt} from '../TaskAttempt';

@Component({
  components: {
    FailureDetails,
  },
})
export default class FailureDetailsModal extends Vue {
  @Inject() private readonly body!: () => FailureDetails;

  private attempt = new TaskAttempt(null, new Task(-1, '', null, null, -1, -1, -1), 0, null, null, null);

  public show(attempt: TaskAttempt) {
    $('#failure-details').modal();
    this.attempt = attempt;
    this.body().show(attempt);
  }

  public hide() {
    $('#failure-details').modal('hide');
  }
}
