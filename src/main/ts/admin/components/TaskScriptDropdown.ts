import {Component, Inject, Vue} from 'vue-property-decorator';
import {ScriptDto} from '../Script';
import Dropdown from '../../components/Dropdown';
import AlertDialog from './AlertDialog';

@Component({
    components: { Dropdown }
})
export default class TaskScriptDropdown extends Vue {
    private readonly defaultOption = {value: null, text: 'Без схемы'};
    public selectedScript: Option = this.defaultOption;
    private scripts: Option[] = [];
    @Inject() private readonly alertDialog!: () => AlertDialog;

    public setSelectedScriptById(id: number | null) {
        this.scripts = [];
        this.selectedScript = this.defaultOption;
        this.scripts.push(this.defaultOption);

        $.ajax({
            url: '/admin/script/all',
        }).done((scripts: ScriptDto[]) => {
            scripts.forEach(script => {
                const option = {
                    value: script.id,
                    text: script.description
                };
                this.scripts.push(option);
                if (option.value === id) {
                    this.selectedScript = option;
                }
            });
        }).fail(xhr => {
            const title = 'Не удалось получить список схем:';
            this.alertDialog().show(title, xhr.statusText);
        });
    }
}

type Option = { value: number | null; text: string };
