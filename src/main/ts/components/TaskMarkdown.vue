<template>
    <div id="taskCodeDescription">
        <div class="row">
            <div class="col-sm-12 col-lg-6 text-left">
                    <div class="form-group">
                        <label for="task-properties-code">Markdown разметка</label>
                        <textarea class="form-control"
                                  v-model="text"
                                  id="task-properties-code" rows="5">
                        </textarea>
                    </div>
            </div>
            <div class="col-sm-12 col-lg-6">
                <label>Preview</label>
                <div v-html="markdownText()"></div>
            </div>
        </div>
    </div>
</template>

<script lang="ts">
import {Component, Vue} from 'vue-property-decorator';
import Showdown from 'showdown';

@Component
export default class TaskMarkdown extends Vue {
    private text = '';
    private converter = new Showdown.Converter();

    public markdownText(): string {
        return this.converter.makeHtml(this.text);
    }
}
</script>
