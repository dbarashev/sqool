<template>
    <div>
        <nav class="navbar navbar-light justify-content-between">
            <AvailableContestsDropdown></AvailableContestsDropdown>
            <div class="dropdown">
                <a class="nav-link dropdown-toggle" href="#"
                   id="userDropdown"
                   data-toggle="dropdown"
                   aria-haspopup="true"
                   aria-expanded="false">
                    {{ userName }}
                </a>
                <div class="dropdown-menu" aria-labelledby="userDropdown">
                    <a class="dropdown-item btn-logout" href="#" @click="logout">Выйти</a>
                </div>
            </div>
        </nav>

        <div class="m-3">
            <AttemptTable ref="attemptTable"></AttemptTable>
        </div>

        <div class="m-5">
            <VariantChooser ref="variantChooser"></VariantChooser>
        </div>
        <TaskAttemptPropertiesModal ref="taskAttemptPropertiesModal"></TaskAttemptPropertiesModal>
    </div>
</template>

<script lang="ts">
import {Component, Provide, Vue} from 'vue-property-decorator';
import AvailableContestsDropdown from './AvailableContestsDropdown.vue';
import VariantChooser from './VariantChooser';
import AttemptTable from './AttemptTable.vue';
import TaskAttemptPropertiesModal from '../components/TaskAttemptPropertiesModal';
import {getUser, User, init as authInit, signOut} from '../../auth/Auth';
import {Landing} from '../../Routes';

@Component({
    components: {
      AttemptTable,
      AvailableContestsDropdown,
      VariantChooser,
      TaskAttemptPropertiesModal,
    },
})
export default class Dashboard extends Vue {
  private userName = window.userName || 'чувак';
  private user: User;

  @Provide()
  public variantChooser(): VariantChooser {
    return this.$refs.variantChooser as VariantChooser;
  }

  @Provide()
  public attemptTable(): AttemptTable {
    return this.$refs.attemptTable as AttemptTable;
  }

  @Provide()
  public taskAttemptProperties(): TaskAttemptPropertiesModal {
    return this.$refs.taskAttemptPropertiesModal as TaskAttemptPropertiesModal;
  }

  public mounted() {
    authInit().then((user) => this.user = user);
  }

  private logout() {
    signOut().then(() => Landing.redirect());
  }
}
</script>

<style lang="scss">
    #userDropdown {
        text-transform: none;
    }
</style>

