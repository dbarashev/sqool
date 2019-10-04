<template>
    <div>
        <table class="table table-hover mt-2">
            <thead class="thead-dark">
            <tr>
                <th scope="col">Название схемы</th>
            </tr>
            </thead>
            <tbody>
            <tr v-for="s in scripts"
                @click="makeActive(s)"
                @dblclick="editScript()"
                v-bind:class="{ 'table-active': s.active }">
                <td><span>{{ s.description }}</span>
                    <a class="btn float-right"
                       data-toggle="collapse"
                       :href="'#scriptBody' + s.id"
                       role="button">
                    Показать SQL
                </a>
                    <div :id="'scriptBody' + s.id" class="collapse m-4 p-2 bg-dark">
                        <pre class="text-light">{{ s.body }}</pre>
                    </div>
                </td>
            </tr>
            </tbody>
        </table>
    </div>
</template>

<script lang="ts" src="./ScriptTable.ts">
</script>

<style lang="scss">
    pre {
        white-space: pre-wrap;
        word-wrap: break-word;
    }
</style>

