import {Component, Vue} from "vue-property-decorator";
import {ScriptDto} from "../Script";

@Component
export default class TaskScriptDropdown extends Vue {
    public selectedScriptId: number | null = null;
    private scripts: ScriptDto[] = [];

    public setSelectedScriptById(id: number | null) {
        this.updateScriptList();
        this.selectedScriptId = id;
    }

    private updateScriptList() {
        $.ajax({
            url: '/admin/script/all',
        }).done((scripts: ScriptDto[]) => {
            this.scripts = [];
            scripts.forEach(script => this.scripts.push(script));
        });
    }
}