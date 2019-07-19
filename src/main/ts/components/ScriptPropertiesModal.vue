<template>
    <div class="modal" tabindex="-1" role="dialog" id="script-properties">
        <div class="modal-dialog" role="document">
            <div class="modal-content">
                <div class="modal-header">
                    <h5 class="modal-title">Схема {{ scriptDescription }}</h5>
                    <button type="button" class="close" data-dismiss="modal" aria-label="Close">
                        <span aria-hidden="true">&times;</span>
                    </button>
                </div>
                <div class="modal-body">
                    <form>
                        <div class="form-group">
                            <label for="script-properties-name">Название</label>
                            <input type="text" class="form-control"
                                   id="script-properties-name"
                                   aria-describedby="script-properties-name-help"
                                   v-model="scriptDescription"
                            >
                        </div>
                    </form>
                    <form>
                        <div class="form-group">
                            <label for="script-file">Код</label>
                            <input type="file" class="form-control-file" id="script-file" @change="readScriptBody">
                        </div>
                    </form>
                </div>
                <div class="modal-footer">
                    <button type="button" class="btn btn-primary" @click="submit">OK</button>
                </div>
            </div>
        </div>
    </div>
</template>

<script lang="ts" src="./ScriptPropertiesModal.ts">
</script>
