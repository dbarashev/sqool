<template>
    <div class="d-flex">
        <button type="button" class="btn btn-raised btn-danger mr-3" @click="createNewContest">Новый Контест</button>
    </div>
</template>

<script lang="ts">
import {Component, Inject, Vue} from 'vue-property-decorator';
import {ContestDto} from '../Contest';
import ContestPropertiesModal from './ContestPropertiesModal';

@Component
export default class ContestToolbar extends Vue {
    @Inject() public readonly contestProperties!: () => ContestPropertiesModal;

    public createNewContest() {
        const newContest = new ContestDto('', '', '', '');
        this.contestProperties().show(newContest).then((updatedContest: ContestDto) => {
            return $.ajax('/admin/contest/new', {
                method: 'POST',
                data: {
                    code: updatedContest.code,
                    name: updatedContest.name,
                    start_ts: updatedContest.start_ts,
                    end_ts: updatedContest.end_ts,
                },
            });
        }).then(() => {
            this.contestProperties().hide();
        });
    }
}
</script>
