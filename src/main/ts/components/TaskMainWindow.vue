<template>
    <div id="task-main-window">
        <TaskTable ref="taskTable" id="task-main-window-task-table">
        </TaskTable>
        <AvailableSolutions ref="availableSolutions" id="task-main-window-available-solutions" style="display: none">
        </AvailableSolutions>
        <ReviewPage ref="reviewPage" id="task-main-window-review-page" style="display: none">
        </ReviewPage>
    </div>
</template>
<script lang="ts" src="./TaskMainWindow.ts"></script>
