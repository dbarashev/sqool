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
import {Contest, VariantPolicy} from '../Contest';
import VariantChooser from './VariantChooser';
import AttemptTable from "./AttemptTable";

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

  private onContestChange(contestOption: ContestOption) {
    const contest = new Contest(contestOption.code, contestOption.variantPolicy);
    if (contestOption.chosenVariant) {
      this.loadTasks(contest);
    } else {
      this.attemptTable().clear();
      this.variantChooser().show(contest, this.loadTasks, this.onFailure);
    }
  }

  private loadTasks = (contest: Contest) => {
    this.variantChooser().hide();
    contest.refreshAttempts()
        .done(() => this.attemptTable().setAttempts(contest.attempts))
        .fail(this.onFailure);
  };

  private onFailure = (xhr: JQuery.jqXHR) => {
    const title = 'Не удалось загрузить вариант:';
    this.alertDialog().show(title, xhr.statusText);
  };
  // private loadVariant(contestOption: ContestOption, variant: VariantOption | null) {
  //   if (!variant && contestOption.variants.length == 1) {
  //     variant = contestOption.variants[0];
  //   }
  //   let ajax;
  //   if (variant) {
  //     ajax = $.ajax({
  //       url: '/acceptVariant',
  //       method: 'POST',
  //       data: {
  //         contest_code: contestOption.code,
  //         variant_id: variant.id
  //       }
  //     })
  //   } else {
  //     ajax = $.ajax({
  //       url: '/acceptContest',
  //       method: 'POST',
  //       data: {contest_code: contestOption.code}
  //     })
  //   }
  //   ajax.done(() => {
  //     const contest = new Contest(contestOption.code);
  //     this.$emit("input", contest);
  //     return contest.refreshAttempts()
  //   }).fail(xhr => {
  //     const title = 'Не удалось загрузить вариант:';
  //     this.alertDialog().show(title, xhr.statusText);
  //   });
  // }
}

class ContestOption {
  constructor(readonly code: string, readonly name: string, readonly variantPolicy: VariantPolicy,
              readonly variants: VariantOption[], readonly chosenVariant?: VariantOption) {
  }
}

class VariantOption {
  constructor(readonly id: number, readonly name: string) {
  }
}
</script>
