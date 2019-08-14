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
            <table class="table table-hover table-striped table-fixed">
                <thead class="thead-dark">
                <tr>
                    <th class="w-25">Задача</th>
                    <th class="w-25">Сложность</th>
                    <th class="w-25">Стоимость</th>
                    <th class="w-25"></th>
                </tr>
                </thead>
                <tbody>
                <tr v-for="attempt in contest.attempts">
                    <td @click="showTaskAttempt(attempt)">{{ attempt.taskEntity.name }}</td>
                    <td @click="showTaskAttempt(attempt)">{{ attempt.taskEntity.difficulty }}</td>
                    <td @click="showTaskAttempt(attempt)">{{ attempt.taskEntity.score }}</td>
                    <td>
                        <div v-if="attempt.status === 'failure'">
                            <a href="#" @click="showFailureDetails(attempt)">{{ getErrorMessage(attempt.count) }}</a>
                        </div>
                        <div v-if="attempt.status === 'virgin'" @click="showTaskAttempt(attempt)">
                            Нет решения
                        </div>
                        <div v-if="attempt.status === 'testing'" @click="showTaskAttempt(attempt)">
                            <i class="fa fa-cog fa-spin fa-fw"></i>Проверяем...
                        </div>
                        <div v-if="attempt.status === 'success'" @click="showTaskAttempt(attempt)">
                            <i class="fa fa-thumbs-up"></i>Решена!
                        </div>
                    </td>
                </tr>
                </tbody>
            </table>
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

@Component({
    components: {AvailableContestsDropdown, VariantChooser},
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

  private showFailureDetails(attempt: TaskAttempt) {
    this.failureDetails().show(attempt);
  }
}
</script>
