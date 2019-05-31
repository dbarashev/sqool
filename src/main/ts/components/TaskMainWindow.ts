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

    public mounted() {
        this.showTaskTable();
    }

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

    public hideChildren() {
        this.taskTable().hide();
        this.availableSolutions().hide();
        this.reviewPage().hide();
    }

    public showAvailableSolutions() {
        this.hideChildren();
        this.availableSolutions().show();
    }

    public showTaskTable() {
        this.hideChildren();
        this.taskTable().show();
    }

    public showReviewPage() {
        this.hideChildren();
        this.reviewPage().show();
    }
}
