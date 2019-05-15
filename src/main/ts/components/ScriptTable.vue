<template>
    <div id="scriptAccordion">

        <div class="card" v-for="s in scripts">
            <div class="card-header" :id="'scriptHeading' + s.id">
                <h5 class="mb-0">
                    <button class="btn btn-link"
                            data-toggle="collapse"
                            :data-target="'#scriptCollapse' + s.id"
                            aria-expanded="false"
                            :aria-controls="'scriptCollapse' + s.id">
                        {{ s.description }}
                    </button>
                </h5>
            </div>

            <div :id="'scriptCollapse' + s.id" class="collapse" :aria-labelledby="'scriptHeading' + s.id" data-parent="#scriptAccordion">
                <div class="card-body">
                    {{ s.body }}
                </div>
            </div>
        </div>
    </div>
</template>

<script lang="ts">
import {Component, Vue} from 'vue-property-decorator';
import {ScriptDto} from '../Script';

@Component
export default class ScriptTable extends Vue {
    public scripts: ScriptDto[] = [
        new ScriptDto(1, 'Марсофлот', 'CREATE TABLE Planet(id INT, NAME TEXT)'),
        new ScriptDto(2, 'Пироги', 'CREATE TABLE Pie(id INT, NAME TEXT)'),
    ];

    public refresh() {
        $.ajax({
            url: '/admin/script/all',
        }).done((scripts: ScriptDto[]) => {
            this.scripts = [];
            scripts.forEach((s) => this.scripts.push(s));
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
