<template>
    <div>
        <ContestToolbar ref="contestToolbar" v-if="visibleComponent === 'contests'">
        </ContestToolbar>
        <ContestTable ref="contestTable" v-if="visibleComponent === 'contests'">
        </ContestTable>

        <AttemptToolbar ref="attemptToolbar" v-if="visibleComponent !== 'contests'">
        </AttemptToolbar>
        <AttemptTableByStudent ref="attemptTableByStudent">
        </AttemptTableByStudent>
        <AttemptTableByTask ref="attemptTableByTask">
        </AttemptTableByTask>

        <ReviewPage ref="reviewPage">
        </ReviewPage>
    </div>
</template>

<script lang="ts" src="./ContestMainWindow.ts">
</script>
