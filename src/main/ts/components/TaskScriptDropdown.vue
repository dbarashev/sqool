<template>
    <div class="form-group">
        <label for="scripts-dropdown">Схема</label>
        <b-form-select id="scripts-dropdown" v-model="selectedScriptId">
            <option value="null">
                {{ 'Без схемы' }}
            </option>
            <option v-for="script in scripts" v-bind:value="script.id">
                {{ script.description }}
            </option>
        </b-form-select>
    </div>
</template>

<script lang="ts" src="./TaskScriptDropdown.ts">
</script>