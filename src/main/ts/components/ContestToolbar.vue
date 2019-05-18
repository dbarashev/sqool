<template>
    <div class="d-flex">
        <button type="button" class="btn btn-raised btn-danger mr-3" @click="createNewContest">Новый Контест</button>
        <button type="button" class="btn btn-secondary mr-3" @click="editContest">Редактировать свойства</button>
    </div>
</template>

<script lang="ts">
import {Component, Inject, Vue} from 'vue-property-decorator';
import {ContestDto} from '../Contest';
import ContestPropertiesModal from './ContestPropertiesModal';
import ContestTable from './ContestTable';

function buildContestPayload(contest: ContestDto): object {
    return {
        method: 'POST',
        data: {
            code: contest.code,
            name: contest.name,
            start_ts: contest.start_ts,
            end_ts: contest.end_ts,
        },
    };
}

@Component
export default class ContestToolbar extends Vue {
    @Inject() public readonly contestProperties!: () => ContestPropertiesModal;
    @Inject() public readonly contestTable!: () => ContestTable;

    public createNewContest() {
        const newContest = new ContestDto('', '', '', '');
        this.contestProperties().show(newContest).then((updatedContest: ContestDto) => {
            return $.ajax('/admin/contest/new', buildContestPayload(updatedContest));
        }).then(() => {
            this.contestProperties().hide();
        });
    }

    public editContest() {
        const activeContest = this.contestTable().getActiveContest();
        if (activeContest) {
            this.contestProperties().show(activeContest).then((updatedContest: ContestDto) => {
                $.ajax('/admin/contest/update', buildContestPayload(updatedContest));
            }).then(() => {
                this.contestProperties().hide();
                this.contestTable().refresh();
            });
        }
    }

}
</script>
