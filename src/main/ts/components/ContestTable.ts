import {ContestDto} from '../Contest';
import { Component, Vue } from 'vue-property-decorator';

@Component
export default class ContestTable extends Vue {
    public contests: ContestDto[] = [];
    private activeContest?: ContestDto;

    public mounted() {
        this.refresh();
    }

    public refresh() {
        $.ajax({
            url: '/admin/contest/all',
        }).done((contests: ContestDto[]) => {
            this.contests = [];
            contests.forEach((c) => this.contests.push(c));
        });

    }

    public getActiveContest(): ContestDto | undefined {
        return this.activeContest;
    }

    public makeActive(contest: ContestDto) {
        contest.active = true;
        if (this.activeContest) {
            this.activeContest.active = false;
        }
        this.activeContest = contest;
        this.$forceUpdate();
    }

}
