<template>
    <div id="available-solutions">
        <table class="table table-hover">
            <thead class="thead-dark">
            <tr>
                <th scope="col">Пользователь</th>
            </tr>
            </thead>
            <tbody>
            <tr v-for="t in solutions">
                <td @click="getReviewPage(t.user_id)">{{ t.user_id }}</td>
            </tr>
            </tbody>
        </table>
    </div>
</template>
<script lang="ts" src="./AvailableSolutions.ts"></script>

<!-- Add "scoped" attribute to limit CSS to this component only -->
<style scoped lang="scss">
    table {
        margin-top: 20px;
    }
</style>
