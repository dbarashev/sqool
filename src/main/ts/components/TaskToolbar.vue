<template>
    <div class="d-flex">
        <button type="button" class="btn btn-raised btn-danger mr-3">New Task</button>
        <button type="button" class="btn btn-secondary">Build Variant</button>
    </div>
</template>

<script lang="ts">
import { Component, Vue } from 'vue-property-decorator';

@Component
export default class TaskToolbar extends Vue {
}
</script>

<!-- Add "scoped" attribute to limit CSS to this component only -->
<style scoped lang="scss">
</style>
