import {Component, Inject, Vue} from 'vue-property-decorator';
import {ScriptDto} from '../Script';
import AlertDialog from "./AlertDialog";

@Component
export default class ScriptPropertiesModal extends Vue {
    @Inject() public readonly alertDialog!: () => AlertDialog;
    public scriptDescription: string = '';
    public selectedFileName: string = '';

    private scriptId: number = -1;
    private scriptBody: string = '';
    private deferredScriptBody: JQueryDeferred<string> | undefined;
    private deferred: JQueryDeferred<ScriptDto> = $.Deferred<ScriptDto>();

    public show(script: ScriptDto): JQueryDeferred<ScriptDto> {
        $('#script-properties').modal();
        this.scriptId = script.id;
        this.scriptDescription = script.description;
        this.scriptBody = script.body;
        this.selectedFileName = '';

        this.deferred = $.Deferred<ScriptDto>();
        this.deferredScriptBody = undefined;
        return this.deferred;
    }

    public hide() {
        $('#script-properties').modal('hide');
    }

    public submit() {
        if (this.deferredScriptBody) {
            this.deferredScriptBody.then(scriptBody => this.resolveScript(scriptBody));
        } else {
            this.resolveScript(this.scriptBody);
        }
    }

    public readScriptBody(event: any) {
        const files = event.target.files;
        if (files && files.length == 0) {
            this.alertDialog().show('Не удалось загрузить файл');
            return;
        }
        const file = files[0];
        this.deferredScriptBody = $.Deferred<string>();

        const reader = new FileReader();
        reader.onload = () => {
            const result = reader.result;
            if (result !== null && this.deferredScriptBody) {
                this.deferredScriptBody.resolve(result as string);
                this.selectedFileName = file.name;
            } else {
                this.rejectScriptBody();
            }
        };
        reader.onerror = this.rejectScriptBody;
        reader.readAsText(file);
    }

    private rejectScriptBody() {
        if (this.deferredScriptBody) {
            this.deferredScriptBody.reject();
            this.alertDialog().show('Не удалось прочитать файл');
        }
    }

    private resolveScript(scriptBody: string) {
        this.deferred.resolve(new ScriptDto(this.scriptId, this.scriptDescription, scriptBody));
    }
}
