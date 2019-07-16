<template>
    <div class="dropdown">
        <button class="btn btn-secondary dropdown-toggle" type="button" data-toggle="dropdown">
            {{ selected.text }}
        </button>
        <div class="dropdown-menu">
            <a class="dropdown-item" href="#" v-for="option in options" @click="selected=option">
                {{ option.text }}
            </a>
        </div>
    </div>
</template>

<script lang="ts" src="./Dropdown.ts">
</script>