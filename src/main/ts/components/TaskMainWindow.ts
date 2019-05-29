import {Vue, Component, Provide} from 'vue-property-decorator';
import AvailableSolutions from './AvailableSolutions';
import ReviewPage from './ReviewPage';
import TaskTable from './TaskTable';


@Component({
    components: {
        ReviewPage,
        TaskTable,
        AvailableSolutions,
    },
})
export default class TaskMainWindow extends Vue {
    @Provide()
    public availableSolutions(): AvailableSolutions {
        return this.$refs.availableSolutions as AvailableSolutions;
    }

    @Provide()
    public reviewPage(): ReviewPage {
        return this.$refs.reviewPage as ReviewPage;
    }

    @Provide()
    public taskTable(): TaskTable {
        return this.$refs.taskTable as TaskTable;
    }

    public hide() {
        $('#task-main-window-task-table').hide();
        $('#task-main-window-available-solutions').hide();
        $('#task-main-window-review-page').hide();
    }

    public showAvailableSolutions() {
        this.hide();
        this.availableSolutions().refresh();
        $('#task-main-window-available-solutions').show();
    }

    public showTaskTable() {
        this.hide();
        this.taskTable().refresh();
        $('#task-main-window-task-table').show();
    }

    public showReviewPage() {
        this.hide();
        $('#task-main-window-review-page').show();
    }
}
