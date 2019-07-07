<template>
    <label>
        <select v-model="selectedScriptId">
            <option value="null">
                {{ 'Без схемы' }}
            </option>
            <option v-for="script in scripts" v-bind:value="script.id">
                {{ script.description }}
            </option>
        </select>
    </label>
</template>

<script lang="ts" src="./TaskScriptTab.ts">
</script>