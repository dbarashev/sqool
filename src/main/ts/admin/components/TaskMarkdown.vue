<template>
    <div id="taskCodeDescription">
        <div class="row">
            <vue-simplemde v-model="text" ref="markdownEditor" />
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
