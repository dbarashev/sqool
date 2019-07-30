<template>
  <div class="bmd-layout-container bmd-drawer-f-l" id="app">
    <AvailableContestsDropdown></AvailableContestsDropdown>
  </div>
</template>

<script lang="ts">
import {Component, Provide, Vue} from 'vue-property-decorator';
import AvailableContestsDropdown from './components/AvailableContestsDropdown.vue';
import AlertDialog from '../components/AlertDialog.vue';

@Component({
  components: {
    AvailableContestsDropdown, AlertDialog
  },
})
export default class App extends Vue {
  @Provide()
  public alertDialog(): AlertDialog {
    return this.$refs.alertDialog as AlertDialog;
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
