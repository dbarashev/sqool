<template>
    <div>
        <ContestToolbar ref="contestToolbar" v-if="visibleComponent === 'contests'">
        </ContestToolbar>
        <ContestTable ref="contestTable" v-if="visibleComponent === 'contests'">
        </ContestTable>

        <AttemptToolbar ref="attemptToolbar" v-if="visibleComponent !== 'contests'">
        </AttemptToolbar>
        <AttemptTable ref="attemptTableByStudent" v-if="visibleComponent === 'attempts-by-student'">
        </AttemptTable>
        <AttemptTableByTask ref="attemptTableByTask" v-if="visibleComponent === 'attempts-by-task'">
        </AttemptTableByTask>
    </div>
</template>

<script lang="ts" src="./ContestMainWindow.ts">
</script>
