<template>
    <div class="modal" tabindex="-1" role="dialog" id="alert-dialog">
        <div class="modal-dialog" role="document">
            <div class="modal-content">
                <div class="modal-header">
                    <h5 class="modal-title">{{ title }}</h5>
                    <button type="button" class="close" data-dismiss="modal" aria-label="Close">
                        <span aria-hidden="true">&times;</span>
                    </button>
                </div>
                <div class="modal-body">
                    <textarea class="form-control" v-model="message" readonly rows="8"></textarea>
                </div>
                <div class="modal-footer">
                    <button type="button" class="btn btn-primary" @click="hide">OK</button>
                </div>
            </div>
        </div>
    </div>
</template>

<script lang="ts" src="./AlertDialog.ts">
</script>

<style scoped>
    #alert-dialog {
        z-index: 2000;
    }
</style>