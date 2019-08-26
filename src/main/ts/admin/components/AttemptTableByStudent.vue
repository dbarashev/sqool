<template>
    <table class="table table-hover">
        <thead class="thead-dark">
        <tr>
            <th scope="col">Студент</th>
        </tr>
        </thead>
        <tbody>
        <tr v-for="student in students">
            <td>
                <span>{{ student.user_name }}</span>
                <a class="btn float-right" data-toggle="collapse"
                   :href="'#' + student.user_id" role="button"
                   @click="refreshUserTable('attemptTable' + student.user_id)">
                    Показать попытки
                </a>
                <div :id="student.user_id" class="collapse m-4">
                    <AttemptTable :ref="'attemptTable' + student.user_id" :contestCode="contest.code" :userId="student.user_id">
                    </AttemptTable>
                </div>
            </td>
        </tr>
        </tbody>
    </table>
</template>

<script lang="ts" src="./AttemptTableByStudent.ts">
</script>