<template>
    <div id="taskCodeDescription">
        <div class="row">
            <vue-simplemde v-model="text" ref="markdownEditor" />
            <!--            <div class="col-sm-12 col-lg-6">-->
<!--                    <div class="form-group">-->
<!--                        <label for="task-properties-code">Markdown разметка</label>-->
<!--                        <textarea class="form-control mt-2"-->
<!--                                  v-model="text"-->
<!--                                  id="task-properties-code" rows="30">-->
<!--                        </textarea>-->
<!--                    </div>-->
<!--            </div>-->
<!--            <div class="col-sm-12 col-lg-6 text-left">-->
<!--                <div class="form-group bmd-form-group">-->
<!--                    <label class="bmd-label-static" for="task-properties-preview">Превью</label>-->
<!--                    <div id="task-properties-preview">-->
<!--                        <div class="card card-body mt-2" v-html="markdownText()"></div>-->
<!--                    </div>-->
<!--                </div>-->
<!--            </div>-->
        </div>
    </div>
</template>
<script lang="js" src="./TaskMarkdown.ts">
</script>
<style lang="scss">
    @import '~simplemde/dist/simplemde.min.css';
    .vue-simplemde {
        .CodeMirror {
            font-family: monospace;
        }
        .CodeMirror-fullscreen {
            position: absolute!important;
        }
        .editor-toolbar.fullscreen {
            position: absolute!important;
        }
        .editor-preview-side {
            position: absolute!important;
        }
    }
</style>
