<template>
    <div>
        <nav class="navbar navbar-light justify-content-between">
            <AvailableContestsDropdown v-on:input="contest = arguments[0]"></AvailableContestsDropdown>
            <div class="dropdown">
                <a class="nav-link dropdown-toggle" href="#"
                   id="userDropdown"
                   data-toggle="dropdown"
                   aria-haspopup="true"
                   aria-expanded="false">
                    {{ userName }}
                </a>
                <div class="dropdown-menu" aria-labelledby="userDropdown">
                    <a class="dropdown-item" href="/logout">Выйти</a>
                </div>
            </div>
        </nav>

        <div class="m-3">
            <AttemptTable ref="attemptTable"></AttemptTable>
        </div>

        <div class="m-5">
            <VariantChooser ref="variantChooser"></VariantChooser>
        </div>
    </div>
</template>

<script lang="ts">
import {Component, Inject, Provide, Vue} from 'vue-property-decorator';
import AvailableContestsDropdown from './AvailableContestsDropdown.vue';
import {Contest} from '../Contest';
import TaskAttemptPropertiesModal from './TaskAttemptPropertiesModal';
import {TaskAttempt} from '../TaskAttempt';
import AlertDialog from '../../components/AlertDialog';
import FailureDetailsModal from './FailureDetailsModal';
import VariantChooser from './VariantChooser';
import AttemptTable from './AttemptTable.vue';

@Component({
    components: {AttemptTable, AvailableContestsDropdown, VariantChooser},
})
export default class Dashboard extends Vue {
  @Inject() private readonly alertDialog!: () => AlertDialog;
  @Inject() private readonly taskAttemptProperties!: () => TaskAttemptPropertiesModal;
  @Inject() private readonly failureDetails!: () => FailureDetailsModal;
  private userName = window.userName || 'чувак';
  private contest = new Contest('', 'RANDOM');

  @Provide()
  public variantChooser(): VariantChooser {
    return this.$refs.variantChooser as VariantChooser;
  }

  @Provide()
  public attemptTable(): AttemptTable {
    return this.$refs.attemptTable as AttemptTable;
  }


  private showTaskAttempt(attempt: TaskAttempt) {
    this.taskAttemptProperties().show(attempt).then((solution) => {
      return $.ajax('/submit.do', this.buildSubmissionPayload(attempt, solution));
    }).done(() => {
      this.contest.refreshAttempts().fail((xhr) => {
        const title = 'Не удалось обновить вариант:';
        this.alertDialog().show(title, xhr.statusText);
      });
    }).fail((xhr) => {
      const title = 'Не удалось проверить решение:';
      this.alertDialog().show(title, xhr.statusText);
    }).always(() => {
      this.taskAttemptProperties().hide();
    });
  }

  private buildSubmissionPayload(attempt: TaskAttempt, solution: string): object {
    return {
      method: 'POST',
      data: {
        'task-id': attempt.taskEntity.id,
        'solution': solution,
        'contest-id': this.contest.contestCode,
      },
    };
  }

  private showFailureDetails(attempt: TaskAttempt) {
    this.failureDetails().show(attempt);
  }
}
</script>
