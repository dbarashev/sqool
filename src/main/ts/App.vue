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
            Здесь будут соревнования
          </div>
          <div class="tab-pane fade show active" id="list-tasks" role="tabpanel">
            <TaskToolbar>
            </TaskToolbar>
            <TaskTable>
            </TaskTable>
          </div>
          <div class="tab-pane fade" id="list-variants" role="tabpanel">
            Здесь будут варианты
          </div>
          <div class="tab-pane fade" id="list-schemes" role="tabpanel">
            Здесь будут схемы БД
          </div>
        </div>
      </div>
    </main>
    <TaskPropertiesModal ref="taskPropertiesModal"></TaskPropertiesModal>
  </div>
</template>

<script lang="ts">
import {Component, Provide, Vue} from 'vue-property-decorator';
  import TaskTable from './components/TaskTable.vue';
  import TaskToolbar from './components/TaskToolbar.vue';
  import TaskPropertiesModal from './components/TaskPropertiesModal.vue';

  @Component({
  components: {
    TaskTable, TaskToolbar, TaskPropertiesModal,
  },
})
export default class App extends Vue {
  @Provide()
  public showTaskProperties() {
    const modal = this.$refs.taskPropertiesModal as TaskPropertiesModal;
    modal.show();
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
