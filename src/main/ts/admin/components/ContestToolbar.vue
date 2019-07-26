<template>
    <div class="d-flex">
        <button type="button" class="btn btn-raised btn-danger mr-3" @click="createNewContest">Новый Контест</button>
        <button type="button" class="btn btn-secondary mr-3" @click="editContest">Редактировать свойства</button>
        <button type="button" class="btn btn-secondary" @click="buildContest">Построить контест</button>
    </div>
</template>

<script lang="ts">
import {Component, Inject, Vue} from 'vue-property-decorator';
import {ContestDto} from '../Contest';
import ContestPropertiesModal from './ContestPropertiesModal';
import ContestTable from './ContestTable';
import ContestBuildingProgressBar from './ContestBuildingProgressBar';
import AlertDialog from './AlertDialog';

function buildContestPayload(contest: ContestDto): object {
    return {
        method: 'POST',
        data: {
            code: contest.code,
            name: contest.name,
            start_ts: contest.start_ts,
            end_ts: contest.end_ts,
            variants: JSON.stringify(contest.variants),
        },
    };
}

@Component
export default class ContestToolbar extends Vue {
    @Inject() public readonly contestProperties!: () => ContestPropertiesModal;
    @Inject() public readonly contestTable!: () => ContestTable;
    @Inject() private readonly contestBuildingProgressBar!: () => ContestBuildingProgressBar;
    @Inject() private readonly alertDialog!: () => AlertDialog;

    public createNewContest() {
        const newContest = new ContestDto('', '', '', '', []);
        this.showAndSubmitContest(newContest, '/admin/contest/new');
    }

    public editContest() {
        const activeContest = this.contestTable().getActiveContest();
        if (activeContest) {
            this.showAndSubmitContest(activeContest, '/admin/contest/update');
        }
    }

    public buildContest() {
        const contest = this.contestTable().getActiveContest();
        if (!contest) {
            return;
        }

        this.contestBuildingProgressBar().show();
        $.post('/admin/contest/build', {
            code: contest.code,
        }).done((result: ImageBuildingResult) => {
            let title = '';
            if (result.status === 'OK') {
                title = 'Вариант успешно создан';
            } else {
                title = 'В имени/решении/спецификации задач найдены синтаксические ошибки:';
            }
            this.alertDialog().show(title, result.message);
        }).fail((xhr) => {
            let title = '';
            if (xhr.status >= 500 && xhr.status < 600) {
                title = 'При создании варианта произошла внутренняя ошибка сервера';
            } else {
                title = `Что-то пошло не так: ${xhr.status}`;
            }
            this.alertDialog().show(title);
        }).always(() => {
            this.contestBuildingProgressBar().hide();
        });
    }

    private showAndSubmitContest(contest: ContestDto, url: string) {
        this.contestProperties().show(contest).then((updatedContest) => {
            contest = updatedContest;
            return $.ajax(url, buildContestPayload(updatedContest));
        }).done(() => {
            this.contestProperties().hide();
            this.contestTable().refresh();
        }).fail((xhr) => {
            // Call it again to be able to make another request
            this.showAndSubmitContest(contest, url);
            const title = `Что-то пошло не так: ${xhr.status}`;
            this.alertDialog().show(title, xhr.statusText);
        });
    }
}

class ImageBuildingResult {
    constructor(readonly status: string, readonly message: string = '') {}
}
</script>
