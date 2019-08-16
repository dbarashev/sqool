<template>
    <div>
        <table class="table table-hover">
            <thead class="thead-dark">
            <tr>
                <th scope="col">Задача</th>
                <th scope="col">Пользователь</th>
                <th scope="col">Имя</th>
                <th scope="col">Статус</th>
                <th scope="col">Номер попытки</th>
                <th scope="col">Ошибка / Результат работы программы</th>
            </tr>
            </thead>
            <tbody>
            <tr v-for="t in solutions" @click="showReviewPage(t.user_id)">
                <td>{{ t.user_name }}</td>
                <td>{{ t.nick }}</td>
                <td>{{ t.status }}</td>
                <td>{{ t.count }}</td>
                <td v-if="t.status === 'failure'">{{ t.error_msg }}</td>
                <td v-else>{{ t.result_set }}</td>
            </tr>
            </tbody>
        </table>
    </div>
</template>
<script lang="ts" src="./AvailableSolutions.ts"></script>

