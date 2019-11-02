<template>
    <table class="table table-hover">
        <tbody>
        <tr v-for="student in students">
            <td>
                <i v-if="student.is_selected" class="material-icons sqool-checkbox" @click="select(student, false)">check_box</i>
                <i v-else class="material-icons  sqool-checkbox" @click="select(student, true)">check_box_outline_blank</i>
                <span>{{ student.user_name }}<sup class="badge badge-pill badge-dark ml-1">{{ student.uni }}</sup></span>
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
<style lang="scss">
    sup {
        font-size: xx-small;
    }
    .sqool-checkbox {
        margin-right: 0.25rem;
        cursor: pointer;
        line-height: 1;
        vertical-align: sub;
        &:hover {
            transform: scale(1.2);
        }
    }

</style>
