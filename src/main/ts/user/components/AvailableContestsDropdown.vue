<template>
    <Dropdown :options="contests" v-model="selectedContest"></Dropdown>
</template>

<script lang="ts">
import {Component, Inject, Vue} from 'vue-property-decorator';
import Dropdown from '../../components/Dropdown';
import AlertDialog from '../../components/AlertDialog';

@Component({
    components: { Dropdown },
})
export default class AvailableContestsDropdown extends Vue {
    private readonly defaultOption = {value: null, text: 'Выбрать контест'};
    public selectedContest: Option = this.defaultOption;
    public contests: Option[] = [];
    @Inject() private readonly alertDialog!: () => AlertDialog;

    public refresh() {
        $.ajax({
            url: '/contest/available/all',
            data: {
                user_id: window.userId
            }
        }).done((contests: Contest[]) => {
            this.contests = [];
            contests.forEach(contest => this.contests.push({
                value: contest.code,
                text: contest.name
            }));
        }).fail(xhr => {
            const title = 'Не удалось получить список контестов:';
            this.alertDialog().show(title, xhr.statusText);
        });
    }

    public mounted() {
        this.refresh();
    }
}

type Option = { value: string | null, text: string };

class Contest {
    constructor(readonly code: string, readonly name: string) {}
}
</script>
