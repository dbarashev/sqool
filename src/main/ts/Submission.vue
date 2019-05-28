<template>
    <div class="bmd-layout-container" id="submission">
        <header class="bmd-layout-header"> </header>
        <div class="bmd-layout-drawer"> </div>
        <main class="bmd-layout-content">
            <div class="container">
                <div> Hi! You can get attempt by userId and taskId </div>
                <div class="form-group">
                    <label for="userId"> userId </label>
                    <input id="userId" v-model="userId" class="form-control">
                </div>
                <div class="form-group">
                    <label for="taskId"> taskId </label>
                    <input id="taskId" v-model="taskId" class="form-control">
                </div>
                <button type="button" class="btn btn-raised btn-danger mr-3" @click="getAttempt">Get attempt</button>
                <button type="button" class="btn btn-raised btn-danger mr-3" @click="getLastReview">Get last review</button>
                <TaskMarkdown ref="taskMarkdown"></TaskMarkdown>
                <button type="button" class="btn btn-raised btn-danger mr-3" @click="save">Save</button>
            </div>
        </main>
    </div>
</template>

<script lang="ts" src="./Submission.ts"></script>


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
