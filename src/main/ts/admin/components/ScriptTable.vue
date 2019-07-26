<template>
    <div id="scriptAccordion" class="text-left">

        <div class="card" v-for="s in scripts">
            <div class="card-header bg-light" :id="'scriptHeading' + s.id">
                <h5 class="mb-0"
                            data-toggle="collapse"
                            :data-target="'#scriptCollapse' + s.id"
                            aria-expanded="false"
                            :aria-controls="'scriptCollapse' + s.id">
                        {{ s.description }}
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
    public scripts: ScriptDto[] = [];

    public mounted() {
        this.refresh();
    }

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
#scriptAccordion {
    .card-header {
        cursor: pointer;
    }

}

</style>
