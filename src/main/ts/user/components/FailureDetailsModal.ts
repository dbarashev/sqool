import {Component, Inject, Vue} from 'vue-property-decorator';
import {Task, TaskAttempt} from '../TaskAttempt';

@Component
export default class FailureDetailsModal extends Vue {
  private columnNames: any[] = [];
  private rows: any[] = [];
  private attempt = new TaskAttempt(null, new Task(-1, '', null, null, -1, -1), 0, null, null, null);

  public show(attempt: TaskAttempt) {
    $('#failure-details').modal();
    this.attempt = attempt;
    this.columnNames = [];
    this.rows = [];
    this.parseResultSet();
  }

  public hide() {
    $('#failure-details').modal('hide');
  }

  private parseResultSet() {
    if (this.attempt.resultSet) {
      const jsonList = JSON.parse(this.attempt.resultSet);
      this.rows = jsonList.slice(1);
      for (const colName of jsonList[0]) {
        if (colName !== 'query_id') {
          this.columnNames.push(colName);
        }
      }
    }
  }
}
