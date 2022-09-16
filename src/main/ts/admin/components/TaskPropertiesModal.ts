import {Component, Vue} from 'vue-property-decorator';
import {getTaskResultSql, TaskDto} from '../Task';
import TaskMarkdown from './TaskMarkdown';
import TaskScriptDropdown from './TaskScriptDropdown';

@Component({
  components: {
    TaskMarkdown, TaskScriptDropdown,
  },
})
export default class TaskPropertiesModal extends Vue {
  public taskName: string = '';
  public taskResult: string = '';
  public taskSolution: string = '';
  public taskHasResult: boolean = true;

  private taskId: number = -1;
  private deferred: JQueryDeferred<TaskDto> = $.Deferred<TaskDto>();

  get markdown(): TaskMarkdown {
    return this.$refs.taskMarkdown as TaskMarkdown;
  }

  get scriptsDropdown(): TaskScriptDropdown {
    return this.$refs.taskScriptDropdown as TaskScriptDropdown;
  }

  public show(task: TaskDto): JQueryDeferred<TaskDto> {
    $('#task-properties').modal();
    this.taskId = task.id;
    this.taskName = task.name;
    this.taskHasResult = task.has_result;
    this.taskResult = getTaskResultSql(task);
    this.markdown.textValue = task.description;
    this.scriptsDropdown.setSelectedScriptById(task.script_id);

    this.taskSolution = task.solution;

    this.deferred = $.Deferred<TaskDto>();
    return this.deferred;
  }

  public hide() {
    $('#task-properties').modal('hide');
  }

  public submit() {
    const taskDescription = this.markdown.textValue;
    const scriptId = this.scriptsDropdown.selectedScript.value;
    this.deferred.resolve(
        new TaskDto(this.taskId, this.taskName, taskDescription, this.taskHasResult, this.taskResult, this.taskSolution, scriptId),
    );
  }
}
