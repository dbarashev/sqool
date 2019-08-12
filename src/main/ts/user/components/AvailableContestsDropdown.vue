<template>
    <div class="dropdown">
        <a class="dropdown-toggle" href="#" id="contests-dropdown" data-toggle="dropdown"
           aria-haspopup="true" aria-expanded="false">
            {{ currentText }}
        </a>
        <div aria-labelledby="contests-dropdown" class="dropdown-menu">
            <div v-for="contest in contests">
                <div class="dropdown-submenu" v-if="contest.variants.length > 1">
                    <a class="dropdown-item dropdown-toggle" href="#">{{ contest.name }}</a>
                    <div class="dropdown-menu">
                        <a class="dropdown-item" href="#" v-for="variant in contest.variants" @click="loadVariant(contest, variant)">
                            {{ variant.name }}
                        </a>
                    </div>
                </div>
                <a class="dropdown-item" href="#" v-if="contest.variants.length <= 1" @click="loadVariant(contest, null)">
                    {{ contest.name }}
                </a>
            </div>
        </div>
    </div>
</template>

<script lang="ts">
import {Component, Inject, Vue} from 'vue-property-decorator';
import AlertDialog from '../../components/AlertDialog';
import {ContestChallenge, VariantChallenge} from "../Challenge";

@Component
export default class AvailableContestsDropdown extends Vue {
  @Inject() private readonly alertDialog!: () => AlertDialog;
  private currentText = 'Выбрать контест';
  private contests: ContestOption[] = [];

  public refresh() {
    $.ajax({
      url: '/contest/available/all'
    }).done((contests: ContestOption[]) => {
      this.contests = contests;
    }).fail(xhr => {
      const title = 'Не удалось получить список контестов:';
      this.alertDialog().show(title, xhr.statusText);
    });
  }

  public mounted() {
    this.refresh();
    $('.dropdown').on('click', '.dropdown-menu .dropdown-toggle', function (e) {
      if (!$(this).next().hasClass('show')) {
        $(this).parents('.dropdown-menu').first().find('.show').removeClass('show');
      }
      $(this).next('.dropdown-menu').toggleClass('show');
      $(this).parents('div.dropdown.show').on('hide.bs.dropdown', function (e) {
        $('.dropdown-submenu .show').removeClass('show');
      });
      return false;
    });
  }

  private loadVariant(contest: ContestOption, variant: VariantOption | null) {
    if (!variant && contest.variants.length == 1) {
      variant = contest.variants[0];
    }
    let challenge;
    if (variant) {
      this.currentText = `${contest.name} / ${variant.name}`;
      challenge = new VariantChallenge(contest.code, variant.id);
    } else {
      this.currentText = contest.name;
      challenge = new ContestChallenge(contest.code);
    }
    challenge.refreshAttempts().fail(xhr => {
      const title = 'Не удалось загрузить вариант:';
      this.alertDialog().show(title, xhr.statusText);
    });
    this.$emit("input", challenge);
  }
}

class ContestOption {
  constructor(readonly code: string, readonly name: string, readonly variants: VariantOption[]) {
  }
}

class VariantOption {
  constructor(readonly id: number, readonly name: string) {
  }
}
</script>

<style scoped>
.dropdown-submenu {
    position: relative;
}

.dropdown-submenu .dropdown-menu {
    top: 0;
    left: 100%;
    margin-left: .1rem;
    margin-right: .1rem;
}
</style>