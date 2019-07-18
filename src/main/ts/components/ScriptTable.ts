import {Component, Vue} from 'vue-property-decorator';
import {ScriptDto} from '../Script';

@Component
export default class ScriptTable extends Vue {
    public scripts: ScriptDto[] = [];
    private activeScript?: ScriptDto;

    public mounted() {
        this.refresh();
    }

    public refresh() {
        $.ajax({
            url: '/admin/script/all',
        }).done((scripts: ScriptDto[]) => {
            this.scripts = [];
            scripts.forEach((s) => this.scripts.push(s));
        });
    }

    public getActiveScript(): ScriptDto | undefined {
        return this.activeScript;
    }

    public makeActive(script: ScriptDto) {
        script.active = true;
        if (this.activeScript) {
            this.activeScript.active = false;
        }
        this.activeScript = script;
        this.$forceUpdate();
    }
}