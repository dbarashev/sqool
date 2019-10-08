<template>
    <div id="taskCodeDescription">
        <div class="row">
            <div class="col-sm-12 col-lg-6">
                    <div class="form-group">
                        <label for="task-properties-code">Markdown разметка</label>
                        <textarea class="form-control mt-2"
                                  v-model="text"
                                  id="task-properties-code" rows="30">
                        </textarea>
                    </div>
            </div>
            <div class="col-sm-12 col-lg-6 text-left">
                <div class="form-group bmd-form-group">
                    <label class="bmd-label-static" for="task-properties-preview">Превью</label>
                    <div id="task-properties-preview">
                        <div class="card card-body mt-2" v-html="markdownText()"></div>
                    </div>
                </div>
            </div>
        </div>
    </div>
</template>
<script lang="ts" src="./TaskMarkdown.ts">
</script>
