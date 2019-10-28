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
                <span>{{ student.user_name }}<sup class="badge badge-pill badge-dark ml-1">{{ student.uni }}</sup></span>
                <a class="btn float-right" data-toggle="collapse"
                   :href="'#' + student.user_id" role="button"
                   @click="refreshUserTable('attemptTable' + student.user_id)">
                    Показать попытки
                </a>
                <a class="btn float-right" role="button"
                   @click="emailReviews(contest.code, student.user_id)">
                    Отослать рецензии
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
<style lang="scss">
    sup {
        font-size: xx-small;
    }
</style>
