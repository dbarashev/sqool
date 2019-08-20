<template>
    <div>
        <ContestToolbar ref="contestToolbar" v-if="isContestTableVisible">
        </ContestToolbar>
        <ContestTable ref="contestTable" v-if="isContestTableVisible">
        </ContestTable>

        <AttemptToolbar ref="attemptToolbar" v-if="isAttemptTableVisible">
        </AttemptToolbar>
        <AttemptTable ref="attemptTableByStudent" v-if="isAttemptTableVisible">
        </AttemptTable>
        <AttemptTable ref="attemptTableByTask" v-if="isAttemptTableVisible">
        </AttemptTable>
    </div>
</template>

<script lang="ts" src="./ContestMainWindow.ts">
</script>
