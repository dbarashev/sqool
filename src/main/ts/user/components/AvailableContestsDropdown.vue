<template>
    <Dropdown id="scripts-dropdown" :options="contests" v-model="selectedContest"></Dropdown>
</template>

<script lang="ts">
import {Component, Vue} from 'vue-property-decorator';
import Dropdown from '../../components/Dropdown';

@Component({
    components: { Dropdown },
})
export default class AvailableContestsDropdown extends Vue {
    public selectedContest: Option = this.defaultOption;
    private readonly defaultOption = {value: null, text: 'Выбрать контест'};
    // TODO: how to get freemarker list of objects?
    private contests: Option[] = [];
}

interface Option { value: string | null; text: string; }
</script>
