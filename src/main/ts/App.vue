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
        <a class="list-group-item list-group-item-action active"
           data-toggle="list" href="#list-contest" role="tab"
           aria-controls="contest">Contests</a>
        <a class="list-group-item list-group-item-action"
           data-toggle="list" href="#list-tasks" role="tab"
           aria-controls="tasks">Tasks</a>
      </div>
    </div>
    <main class="bmd-layout-content">
      <div class="container">
        <div class="tab-content" id="nav-tabContent">
          <div class="tab-pane fade show active" id="list-contest" role="tabpanel">
            Contests go here
          </div>
          <div class="tab-pane fade" id="list-tasks" role="tabpanel">
            Tasks go here
          </div>
        </div>
      </div>
    </main>
  </div>
</template>

<script lang="ts">
import { Component, Vue } from 'vue-property-decorator';
import HelloWorld from './components/HelloWorld.vue';

@Component({
  components: {
    HelloWorld,
  },
})
export default class App extends Vue {}
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
