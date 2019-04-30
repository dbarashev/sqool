import { Component, Vue } from 'vue-property-decorator';
import {ContestDto} from '../Contest';

@Component
export default class ContestPropertiesModal extends Vue {
    public contestName: string = '';
    public contestCode: string = '';
    public contestStart: string = '';
    public contestEnd: string = '';

    private deferred: JQueryDeferred<ContestDto> | undefined;

    public show(contest: ContestDto): JQueryDeferred<ContestDto> {
        $('#contest-properties').modal();
        this.contestName = contest.name;
        this.contestCode = contest.code;
        this.contestStart = contest.start_ts;
        this.contestEnd = contest.end_ts;

        this.deferred = $.Deferred<ContestDto>();
        return this.deferred;
    }

    public hide() {
        $('#contest-properties').modal('hide');
    }

    public submit() {
        if (this.deferred) {
            this.deferred.resolve(new ContestDto(
                this.contestCode, this.contestName, this.contestStart, this.contestEnd));
        }
    }
}
