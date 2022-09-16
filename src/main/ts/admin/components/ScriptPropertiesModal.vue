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
                                   v-model="scriptDescription"
                            >
                        </div>
                    </form>
                    <form>
                        <div class="form-group">
                            <label for="file">Файл</label>
                            <div class="input-group">
                                <input id="file" type="text" class="form-control" placeholder="Выберите SQL файл" v-model="selectedFileName" readonly>
                                <label class="input-group-append file-chooser mb-0">
                                    <span class="btn btn-secondary mb-0">Выбрать...
                                        <input type="file" style="display: none;" @change="readScriptBody">
                                    </span>
                                </label>
                            </div>
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
<style>
    #file {
        background-color: transparent;
        color: grey;
        font-size: smaller;
    }
    #file::placeholder {
        color: grey;
        font-size: smaller;
        padding: 0 1rem;
    }
</style>
