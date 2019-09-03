<template>
    <div class="dropdown">
        <a class="dropdown-toggle nav-link" href="#" id="contests-dropdown" data-toggle="dropdown"
           aria-haspopup="true" aria-expanded="false">
            {{ currentText }}
        </a>
        <div aria-labelledby="contests-dropdown" class="dropdown-menu">
            <div v-for="contest in contests">
                <a class="dropdown-item" href="#" @click="onContestChange(contest)">
                    {{ contest.name }}
                </a>
            </div>
        </div>
    </div>
</template>

<script lang="ts">
import {Component, Inject, Vue} from 'vue-property-decorator';
import AlertDialog from '../../components/AlertDialog';
import {Contest, VariantOption, VariantPolicy} from '../Contest';
import VariantChooser from './VariantChooser';
import AttemptTable from './AttemptTable';

@Component
export default class AvailableContestsDropdown extends Vue {
  @Inject() private readonly alertDialog!: () => AlertDialog;
  @Inject() private readonly variantChooser!: () => VariantChooser;
  @Inject() private readonly attemptTable!: () => AttemptTable;
  private currentText = 'Выбрать контест';
  private contests: ContestOption[] = [];

  public refresh() {
    $.ajax({
      url: '/contest/available/all',
    }).done((contests: ContestOption[]) => {
      this.contests = contests;
    }).fail((xhr) => {
      const title = 'Не удалось получить список контестов:';
      this.alertDialog().show(title, xhr.statusText);
    });
  }

  public mounted() {
    this.refresh();
  }

  public onContestChange(contestOption: ContestOption) {
    const contest = new Contest(contestOption.code, contestOption.variantPolicy, contestOption.variants, contestOption.chosenVariant);
    if (contestOption.chosenVariant) {
      this.loadTasks(contest);
    } else {
      this.attemptTable().clear();
      const onVariantChoice = (newContest: Contest) => {
        this.refresh();
        this.loadTasks(newContest);
      };
      this.variantChooser().show(contest, onVariantChoice, this.onFailure);
    }
  }

  private loadTasks = (contest: Contest) => {
    this.variantChooser().hide();
    contest.refreshAttempts()
        .done(() => this.attemptTable().setContest(contest))
        .fail(this.onFailure);
  }

  private onFailure = (xhr: JQuery.jqXHR) => {
    const title = 'Не удалось загрузить вариант:';
    this.alertDialog().show(title, xhr.statusText);
  }
}

class ContestOption {
  constructor(readonly code: string, readonly name: string, readonly variantPolicy: VariantPolicy,
              readonly variants: VariantOption[], readonly chosenVariant?: VariantOption) {
  }
}
</script>
