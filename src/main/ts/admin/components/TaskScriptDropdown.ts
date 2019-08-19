import {Component, Inject, Vue} from 'vue-property-decorator';
import {ScriptDto} from '../Script';
import Dropdown from '../../components/Dropdown';
import AlertDialog from '../../components/AlertDialog';

const NO_SCRIPT_OPTION = {value: null, text: 'Без схемы'};

@Component({
  components: {Dropdown},
})
export default class TaskScriptDropdown extends Vue {
  public selectedScript: Option = NO_SCRIPT_OPTION;
  private scripts: Option[] = [];
  @Inject() private readonly alertDialog!: () => AlertDialog;

  public setSelectedScriptById(id: number | null) {
    this.scripts = [];
    this.selectedScript = NO_SCRIPT_OPTION;
    this.scripts.push(NO_SCRIPT_OPTION);

    $.ajax({
      url: '/admin/script/all',
    }).done((scripts: ScriptDto[]) => {
      scripts.forEach((script) => {
        const option = {
          value: script.id,
          text: script.description,
        };
        this.scripts.push(option);
        if (option.value === id) {
          this.selectedScript = option;
        }
      });
    }).fail((xhr) => {
      const title = 'Не удалось получить список схем:';
      this.alertDialog().show(title, xhr.statusText);
    });
  }

}

interface Option { value: number | null; text: string; }
