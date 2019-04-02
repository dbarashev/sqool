<template>
    <table class="table table-hover">
        <thead class="thead-dark">
        <tr>
            <th scope="col">Код</th>
            <th scope="col">Название</th>
            <th scope="col">Начало</th>
            <th scope="col">Окончание</th>
        </tr>
        </thead>
        <tbody>
        <tr v-for="c in contests">
            <td>{{ c.code }}</td>
            <td>{{ c.name }}</td>
            <td>{{ c.start_ts }}</td>
            <td>{{ c.end_ts }}</td>
        </tr>
        </tbody>
    </table>
</template>

<script lang="ts">
import { Component, Vue } from 'vue-property-decorator';
import {ContestDto} from '../Contest';

@Component
export default class ContestTable extends Vue {
    public contests: ContestDto[] = [];

    public mounted() {
        $.ajax({
            url: '/admin/contest/all',
        }).done((contests: ContestDto[]) => {
            this.contests = contests;
        });
    }
}
</script>

<!-- Add "scoped" attribute to limit CSS to this component only -->
<style scoped lang="scss">
    table {
        margin-top: 20px;
    }
</style>
