<template>
  <div class="bmd-layout-container bmd-drawer-f-l bmd-drawer-in" id="app">
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
    <div id="dw-s1" class="bmd-layout-drawer">
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
            <ContestToolbar ref="contestToolbar">
            </ContestToolbar>
            <ContestTable ref="contestTable">
            </ContestTable>
          </div>
          <div class="tab-pane fade show active" id="list-tasks" role="tabpanel">
            <TaskToolbar ref="taskToolbar">
            </TaskToolbar>
            <TaskMainWindow ref="taskMainWindow">
            </TaskMainWindow>
          </div>
          <div class="tab-pane fade" id="list-variants" role="tabpanel">
            <VariantToolbar ref="variantToolbar">
            </VariantToolbar>
            <VariantTable ref="variantTable">
            </VariantTable>
          </div>
          <div class="tab-pane fade" id="list-schemes" role="tabpanel">
            <ScriptToolbar ref="scriptToolbar">
            </ScriptToolbar>
            <ScriptTable ref="scriptTable">
            </ScriptTable>
          </div>
        </div>
      </div>
    </main>
    <ContestPropertiesModal ref="contestPropertiesModal"></ContestPropertiesModal>
    <AlertDialog ref="alertDialog"></AlertDialog>
    <TaskPropertiesModal ref="taskPropertiesModal"></TaskPropertiesModal>
    <ContestBuildingProgressBar ref="contestBuildingProgressBar"></ContestBuildingProgressBar>
    <ScriptPropertiesModal ref="scriptPropertiesModal"></ScriptPropertiesModal>
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
import AlertDialog from '../components/AlertDialog.vue';
import ScriptTable from './components/ScriptTable.vue';
import ScriptToolbar from './components/ScriptToolbar.vue';
import ScriptPropertiesModal from './components/ScriptPropertiesModal.vue';
import TaskMainWindow from './components/TaskMainWindow';
import VariantToolbar from './components/VariantToolbar.vue';
import VariantPropertiesModal from './components/VariantPropertiesModal.vue';
import VariantTable from './components/VariantTable';

@Component({
  components: {
    ScriptTable,
    ContestToolbar,
    ContestTable, ContestPropertiesModal,
    TaskToolbar, TaskPropertiesModal,
    ContestBuildingProgressBar, AlertDialog,
    TaskMainWindow, ScriptToolbar, ScriptPropertiesModal,
    VariantTable, VariantToolbar, VariantPropertiesModal,
  },
})
export default class App extends Vue {
  @Provide()
  public taskProperties(): TaskPropertiesModal {
    return this.$refs.taskPropertiesModal as TaskPropertiesModal;
  }

  @Provide()
  public scriptProperties(): ScriptPropertiesModal {
    return this.$refs.scriptPropertiesModal as ScriptPropertiesModal;
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
  public variantToolbar(): VariantToolbar {
    return this.$refs.variantToolbar as VariantToolbar;
  }

  @Provide()
  public contestBuildingProgressBar(): ContestBuildingProgressBar {
    return this.$refs.contestBuildingProgressBar as ContestBuildingProgressBar;
  }

  @Provide()
  public contestToolbar(): ContestToolbar {
    return this.$refs.contestToolbar as ContestToolbar;
  }

  @Provide()
  public alertDialog(): AlertDialog {
    return this.$refs.alertDialog as AlertDialog;
  }

  @Provide()
  public taskMainWindow(): TaskMainWindow {
    return this.$refs.taskMainWindow as TaskMainWindow;
  }

  @Provide()
  public taskToolbar(): TaskToolbar {
    return this.$refs.taskToolbar as TaskToolbar;
  }

  @Provide()
  public scriptTable(): ScriptTable {
    return this.$refs.scriptTable as ScriptTable;
  }

  @Provide()
  public scriptToolbar(): ScriptToolbar {
    return this.$refs.scriptToolbar as ScriptToolbar;
  }

}
</script>

<style lang="scss">
  #app {
    font-family: 'Avenir', Helvetica, Arial, sans-serif;
    -webkit-font-smoothing: antialiased;
    -moz-osx-font-smoothing: grayscale;
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
