<template>
    <div id="taskCodeDescription">
        <div class="row">
            <div class="col-sm-6">
                <form>
                    <div class="form-group">
                        <label for="task-properties-code">Код с комментариями</label>
                        <textarea class="form-control"
                                  v-model="text"
                                  id="task-properties-code" rows="5">
                        </textarea>
                        <small id="task-properties-description-help"
                               class="form-text text-muted">Можно использовать Markdown
                        </small>
                    </div>
                </form>
            </div>
            <div class="col-sm-6">
                <div v-html="markdownText"></div>
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

    get markdownText() {
        return this.converter.makeHtml(this.text);
    }
}
</script>
