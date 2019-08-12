<template>
  <div class="bmd-layout-container bmd-drawer-f-l" id="app">
    <Dashboard></Dashboard>
    <TaskAttemptPropertiesModal ref="taskAttemptPropertiesModal"></TaskAttemptPropertiesModal>
    <FailureDetailsModal ref="failureDetailsModal"></FailureDetailsModal>
    <AlertDialog ref="alertDialog"></AlertDialog>
  </div>
</template>

<script lang="ts">
import {Component, Provide, Vue} from 'vue-property-decorator';
import Dashboard from './components/Dashboard.vue'
import TaskAttemptPropertiesModal from './components/TaskAttemptPropertiesModal';
import FailureDetailsModal from "./components/FailureDetailsModal";
import AlertDialog from '../components/AlertDialog.vue';

@Component({
  components: {
    Dashboard, AlertDialog, TaskAttemptPropertiesModal, FailureDetailsModal
  },
})
export default class App extends Vue {
  @Provide()
  public alertDialog(): AlertDialog {
    return this.$refs.alertDialog as AlertDialog;
  }

  @Provide()
  public taskAttemptProperties(): TaskAttemptPropertiesModal {
    return this.$refs.taskAttemptPropertiesModal as TaskAttemptPropertiesModal;
  }

  @Provide()
  public failureDetails(): FailureDetailsModal {
    return this.$refs.failureDetailsModal as FailureDetailsModal;
  }
}
</script>

<style lang="scss">
  #app {
    font-family: 'Avenir', Helvetica, Arial, sans-serif;
    -webkit-font-smoothing: antialiased;
    -moz-osx-font-smoothing: grayscale;
    text-align: center;
    color: #2c3e50;
    min-height: 100vh;
  }
  .bmd-layout-canvas {
    position: absolute;
    top: 0;
    bottom: 0;
    left: 0;
    right: 0;
  }
</style>
