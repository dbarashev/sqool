import {Vue, Component, Provide} from 'vue-property-decorator';
import TaskTable from './TaskTable';


@Component({
  components: {
    TaskTable,
  },
})
export default class TaskMainWindow extends Vue {

  public mounted() {
    this.showTaskTable();
  }

  @Provide()
  public taskTable(): TaskTable {
    return this.$refs.taskTable as TaskTable;
  }

  public showTaskTable() {
    this.taskTable().show();
  }

}
