import {Component, Inject, Vue} from 'vue-property-decorator';
import {ScriptDto} from '../Script';
import AlertDialog from '../../components/AlertDialog';

@Component
export default class ScriptTable extends Vue {
  public scripts: ScriptDto[] = [];
  private activeScript?: ScriptDto;
  @Inject() private readonly alertDialog!: () => AlertDialog;

  public mounted() {
    this.refresh();
  }

  public refresh() {
    $.ajax({
      url: '/admin/script/all',
    }).done((scripts: ScriptDto[]) => {
      this.scripts = [];
      scripts.forEach((s) => this.scripts.push(s));
    }).fail((xhr) => {
      const title = 'Не удалось получить список схем:';
      this.alertDialog().show(title, xhr.statusText);
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
