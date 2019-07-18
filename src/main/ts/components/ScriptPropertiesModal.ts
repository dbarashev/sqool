import { Component, Vue } from 'vue-property-decorator';
import {ScriptDto} from '../Script';

@Component
export default class ScriptPropertiesModal extends Vue {
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
            this.deferred.resolve(new ScriptDto(this.scriptId, this.scriptDescription, this.scriptDescription));
        }
    }
}
