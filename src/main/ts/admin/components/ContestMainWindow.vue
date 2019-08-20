<template>
    <div>
        <ContestToolbar ref="contestToolbar">
        </ContestToolbar>
        <ContestTable ref="contestTable">
        </ContestTable>

        <AttemptToolbar ref="attemptToolbar">
        </AttemptToolbar>
        <AttemptTable ref="attemptTableByStudent">
        </AttemptTable>
        <AttemptTable ref="attemptTableByTask">
        </AttemptTable>
    </div>
</template>

<script lang="ts" src="./ContestMainWindow.ts">
</script>