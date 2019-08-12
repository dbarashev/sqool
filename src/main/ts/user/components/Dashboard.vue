<template>
    <div>
        <nav class="navbar navbar-expand-md navbar-light">
            <div class="collapse navbar-collapse w-100 order-1 order-md-0">
                <ul class="navbar-nav mr-auto">
                    <li class="nav-item">
                        <AvailableContestsDropdown v-on:input="challenge = arguments[0]"></AvailableContestsDropdown>
                    </li>
                </ul>
            </div>
            <h1 class="navbar-brand mx-auto order-0">Привет, {{ userName }}!</h1>
            <div class="navbar-collapse collapse w-100 order-3">
                <ul class="navbar-nav ml-auto">
                    <li class="nav-item">
                        <a class="nav-link" href="/logout">Выйти</a>
                    </li>
                </ul>
            </div>
        </nav>

        <div class="margin">
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
                <tr v-for="attempt in challenge.attempts">
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
    </div>
</template>

<script lang="ts">
import {Component, Inject, Vue} from 'vue-property-decorator';
import AvailableContestsDropdown from './AvailableContestsDropdown.vue';
import {Challenge} from '../Challenge';
import TaskAttemptPropertiesModal from './TaskAttemptPropertiesModal';
import {TaskAttempt} from '../TaskAttempt';
import AlertDialog from '../../components/AlertDialog';
import FailureDetailsModal from './FailureDetailsModal';

@Component({
    components: {AvailableContestsDropdown}
})
export default class Dashboard extends Vue {
  @Inject() private readonly alertDialog!: () => AlertDialog;
  @Inject() private readonly taskAttemptProperties!: () => TaskAttemptPropertiesModal;
  @Inject() private readonly failureDetails!: () => FailureDetailsModal;
  private userName = window.userName || 'чувак';
  private challenge = new Challenge();

  private showTaskAttempt(attempt: TaskAttempt) {
    this.taskAttemptProperties().show(attempt).then(solution => {
      return $.ajax('/submit.do', this.buildSubmissionPayload(attempt, solution));
    }).done(() => {
      this.challenge.refreshAttempts();
    }).fail(xhr => {
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
        solution: solution,
        'contest-id': this.challenge.contestCode
      }
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
        return 'Это фиаско, друг!'
    }
  }

  private showFailureDetails(attempt: TaskAttempt) {
    this.failureDetails().show(attempt);
  }
}
</script>

<style scoped>
h1 {
    margin: 0;
    padding: 0;
}

.margin {
    margin: 25px 40px;
}
</style>