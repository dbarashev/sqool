<template>
    <div class="modal" tabindex="-1" role="dialog" id="failure-details">
        <div class="modal-dialog" role="document">
            <div class="modal-content">
                <div class="modal-header">
                    <h5 class="modal-title">Задача {{ attempt.taskEntity.name }} решена неверно</h5>
                    <button type="button" class="close" data-dismiss="modal" aria-label="Close">
                        <span aria-hidden="true">&times;</span>
                    </button>
                </div>
                <div class="modal-body">
                    <FailureDetails/>
                </div>
                <div class="modal-footer">
                    <button type="button" class="btn btn-primary" @click="hide">OK</button>
                </div>
            </div>
        </div>
    </div>
</template>

<script lang="ts" src="./FailureDetailsModal.ts">
</script>
