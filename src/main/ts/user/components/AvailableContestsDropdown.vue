<template>
    <Dropdown :options="contests" v-model="selectedContest"></Dropdown>
</template>

<script lang="ts">
import {Component, Vue} from 'vue-property-decorator';
import Dropdown from '../../components/Dropdown';

@Component({
    components: { Dropdown },
})
export default class AvailableContestsDropdown extends Vue {
    private readonly defaultOption = {value: null, text: 'Выбрать контест'};
    public selectedContest: Option = this.defaultOption;
    public contests: Option[];

    constructor() {
        super();
        const contestsJson = $("#contests").val() || "";
        const contests: Contest[] = <Contest[]>JSON.parse(contestsJson.toString());
        this.contests = contests.map(contest => ({
            value: contest.code,
            text: contest.name
        }));
        console.log(contests);
        console.log(this.contests);
    }
}

type Option = { value: string | null, text: string };

class Contest {
    constructor(readonly code: string, readonly name: string) {}
}
</script>
