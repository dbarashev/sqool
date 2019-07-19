import {Component, Inject, Vue} from 'vue-property-decorator';
import {ScriptDto} from '../Script';
import AlertDialog from "./AlertDialog";

@Component
export default class ScriptPropertiesModal extends Vue {
    @Inject() public readonly alertDialog!: () => AlertDialog;
    public scriptDescription: string = '';

    private scriptId: number = -1;
    private scriptBody: string = '';
    private deferred: JQueryDeferred<ScriptDto> | undefined;

    public show(script: ScriptDto): JQueryDeferred<ScriptDto> {
        $('#script-properties').modal();
        this.scriptId = script.id;
        this.scriptDescription = script.description;
        this.scriptBody = script.body;

        this.deferred = $.Deferred<ScriptDto>();
        return this.deferred;
    }

    public hide() {
        $('#script-properties').modal('hide');
    }

    public submit() {
        if (this.deferred) {
            this.deferred.resolve(new ScriptDto(this.scriptId, this.scriptDescription, this.scriptBody));
        }
    }

    public readScriptBody(event: any) {
        const file = event.target.files[0];
        if (!file) {
            this.alertDialog().show('Не удалось загрузить файл');
            return;
        }

        const reader = new FileReader();
        reader.onload = () => {
            const result = reader.result;
            if (result !== null) {
                this.scriptBody = result as string;
            } else {
                this.alertDialog().show('Не удалось прочитать файл');
            }
        };
        reader.readAsText(file)
    }
}
