<template>
  <div class="bmd-layout-container bmd-drawer-f-l" id="app">
    <header class="bmd-layout-header">
      <div class="navbar navbar-light bg-faded">
        <button class="navbar-toggler" type="button" data-toggle="drawer" data-target="#dw-s1">
          <span class="sr-only">Toggle drawer</span>
          <i class="material-icons">menu</i>
        </button>
        <ul class="nav navbar-nav">
          <li class="nav-item">SQooL</li>
        </ul>
      </div>
    </header>
    <div id="dw-s1" class="bmd-layout-drawer bg-faded">
      <header>
        <a class="navbar-brand"></a>
      </header>
      <div class="list-group" id="list-tab" role="tablist">
        <a class="list-group-item list-group-item-action"
           data-toggle="list" href="#list-schemes" role="tab"
           aria-controls="schemes">Схемы</a>
        <a class="list-group-item list-group-item-action active"
           data-toggle="list" href="#list-tasks" role="tab"
           aria-controls="tasks">Задачи</a>
        <a class="list-group-item list-group-item-action"
           data-toggle="list" href="#list-variants" role="tab"
           aria-controls="variants">Варианты</a>
        <a class="list-group-item list-group-item-action"
           data-toggle="list" href="#list-contest" role="tab"
           aria-controls="contest">Соревнования</a>
      </div>
    </div>
    <main class="bmd-layout-content">
      <div class="container">
        <div class="pt-4 tab-content" id="nav-tabContent">
          <div class="tab-pane fade" id="list-contest" role="tabpanel">
            <ContestToolbar>
            </ContestToolbar>
            <ContestTable ref="contestTable">
            </ContestTable>
          </div>
          <div class="tab-pane fade show active" id="list-tasks" role="tabpanel">
            <TaskToolbar>
            </TaskToolbar>
            <TaskMainWindow ref="taskMainWindow">
            </TaskMainWindow>
          </div>
          <div class="tab-pane fade" id="list-variants" role="tabpanel">
            <VariantToolbar>
            </VariantToolbar>
            <VariantTable ref="variantTable">
            </VariantTable>
          </div>
          <div class="tab-pane fade" id="list-schemes" role="tabpanel">
            <ScriptTable>
            </ScriptTable>
          </div>
        </div>
      </div>
    </main>
    <ContestPropertiesModal ref="contestPropertiesModal"></ContestPropertiesModal>
    <AlertDialog ref="alertDialog"></AlertDialog>
    <TaskPropertiesModal ref="taskPropertiesModal"></TaskPropertiesModal>
    <ContestBuildingProgressBar ref="contestBuildingProgressBar"></ContestBuildingProgressBar>
    <VariantBuildingProgressBar ref="variantBuildingProgressBar"></VariantBuildingProgressBar>
    <VariantPropertiesModal ref="variantPropertiesModal"></VariantPropertiesModal>
  </div>
</template>

<script lang="ts">
import {Component, Provide, Vue} from 'vue-property-decorator';
import TaskToolbar from './components/TaskToolbar.vue';
import TaskPropertiesModal from './components/TaskPropertiesModal.vue';
import ContestTable from './components/ContestTable';
import ContestToolbar from './components/ContestToolbar.vue';
import ContestPropertiesModal from './components/ContestPropertiesModal.vue';
import ContestBuildingProgressBar from './components/ContestBuildingProgressBar.vue';
import AlertDialog from './components/AlertDialog.vue';
import ScriptTable from './components/ScriptTable.vue';
import TaskMainWindow from './components/TaskMainWindow';
import VariantToolbar from './components/VariantToolbar.vue';
import VariantPropertiesModal from './components/VariantPropertiesModal.vue';
import VariantTable from './components/VariantTable'

@Component({
  components: {
    ScriptTable,
    ContestToolbar,
    ContestTable, ContestPropertiesModal,
    TaskToolbar, TaskPropertiesModal,
    ContestBuildingProgressBar, AlertDialog,
    TaskMainWindow, VariantToolbar, VariantPropertiesModal,
    VariantTable
  },
})
export default class App extends Vue {
  @Provide()
  public taskProperties(): TaskPropertiesModal {
    return this.$refs.taskPropertiesModal as TaskPropertiesModal;
  }

  @Provide()
  public contestProperties(): ContestPropertiesModal {
    return this.$refs.contestPropertiesModal as ContestPropertiesModal;
  }

  @Provide()
  public variantProperties(): VariantPropertiesModal {
    return this.$refs.variantPropertiesModal as VariantPropertiesModal;
  }

  @Provide()
  public contestTable(): ContestTable {
    return this.$refs.contestTable as ContestTable;
  }

  @Provide()
  public variantTable(): VariantTable {
    return this.$refs.variantTable as VariantTable;
  }

  @Provide()
  public contestBuildingProgressBar(): ContestBuildingProgressBar {
    return this.$refs.contestBuildingProgressBar as ContestBuildingProgressBar;
  }

  @Provide()
  public alertDialog(): AlertDialog {
    return this.$refs.alertDialog as AlertDialog;
  }

  @Provide()
  public taskMainWindow(): TaskMainWindow {
    return this.$refs.taskMainWindow as TaskMainWindow;
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
  }
  .bmd-layout-canvas {
    position: absolute;
    top: 0;
    bottom: 0;
    left: 0;
    right: 0;
  }
</style>
