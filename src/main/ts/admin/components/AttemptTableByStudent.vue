<template>
    <table class="table table-hover">
        <thead class="thead-dark">
        <tr>
            <th scope="col">Студент</th>
        </tr>
        </thead>
        <tbody>
        <tr v-for="student in students">
            <td data-toggle="collapse" :href="'#' + student.user_id">
                {{ student.user_name }}
                <div :id="student.user_id" class="collapse m-2">
                    <AttemptTable :contestCode="contest.code" :userId="student.user_id"></AttemptTable>
                </div>
            </td>
        </tr>
        </tbody>
    </table>
</template>

<script lang="ts" src="./AttemptTableByStudent.ts">
</script>