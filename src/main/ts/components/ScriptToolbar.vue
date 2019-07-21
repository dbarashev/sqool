<template>
    <div class="d-flex">
        <button type="button" class="btn btn-raised btn-danger mr-3" @click="createNewScript">Новая Схема</button>
        <button type="button" class="btn btn-secondary mr-3" @click="editScript">Редактировать свойства</button>
    </div>
</template>

<script lang="ts">
import {Component, Inject, Vue} from 'vue-property-decorator';
import {ScriptDto} from "../Script";
import ScriptTable from "./ScriptTable";
import AlertDialog from "./AlertDialog";
import ScriptPropertiesModal from "./ScriptPropertiesModal";

@Component
export default class ScriptToolbar extends Vue {
    @Inject() public readonly scriptProperties!: () => ScriptPropertiesModal;
    @Inject() public readonly scriptTable!: () => ScriptTable;
    @Inject() public readonly alertDialog!: () => AlertDialog;

    public createNewScript() {
        const newScript = new ScriptDto(-1, '', '');
        this.showAndSubmitScript(newScript, '/admin/script/new');
    }

    public editScript() {
        const activeScript = this.scriptTable().getActiveScript();
        if (activeScript) {
            this.showAndSubmitScript(activeScript, '/admin/script/update');
        }
    }

    private showAndSubmitScript(script: ScriptDto, url: string) {
        this.scriptProperties().show(script).then(updatedScript => {
            script = updatedScript;
            return $.ajax(url, this.buildScriptPayload(updatedScript))
        }).done(() => {
            this.scriptProperties().hide();
            this.scriptTable().refresh();
        }).fail(xhr => {
            // Call it again to be able to make another request
            this.showAndSubmitScript(script, url);
            const title = `Что-то пошло не так: ${xhr.status}`;
            this.alertDialog().show(title, xhr.statusText);
        });
    }

    private buildScriptPayload(script: ScriptDto): object {
        return {
            method: 'POST',
            data: {
                id: script.id,
                description: script.description,
                body: script.body,
            },
        };
    }
}
</script>