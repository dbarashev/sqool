<template>
    <div class="bmd-layout-container" id="submission">
        <header class="bmd-layout-header"> </header>
        <div class="bmd-layout-drawer"> </div>
        <main class="bmd-layout-content">
            <div class="container">
                <div> Hi! You can get attempt by userId and taskId </div>
                <div class="form-group">
                    <label for="userId"> userId </label>
                    <input id="userId" v-bind:value="-1"
                           v-on:input="reviewPage().userId = $event.target.value" class="form-control">
                </div>
                <div class="form-group">
                    <label for="taskId"> taskId </label>
                    <input id="taskId" v-bind:value="-1"
                           v-on:input="reviewPage().taskId = $event.target.value" class="form-control">
                </div>
                <ReviewPage ref="reviewPage"></ReviewPage>
            </div>
        </main>
    </div>
</template>
<script lang="ts" src="./Submission.ts"></script>
