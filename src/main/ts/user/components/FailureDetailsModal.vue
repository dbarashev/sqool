<template>
    <div class="modal" tabindex="-1" role="dialog" id="failure-details">
        <div class="modal-dialog" role="document">
            <div class="modal-content">
                <div class="modal-header">
                    <h5 class="modal-title">Задача {{ attempt.taskEntity.name }} решена неверно</h5>
                    <button type="button" class="close" data-dismiss="modal" aria-label="Close">
                        <span aria-hidden="true">&times;</span>
                    </button>
                </div>
                <div class="modal-body">
                    <form>
                        <div class="form-group text-left">
                            <label class="bmd-label-static" for="error-message">Сообщение об ошибке</label>
                            <pre id="error-message">{{ attempt.errorMsg }}</pre>
                        </div>
                        <div class="form-group">
                            <div class="text-left">
                                <label class="bmd-label-static" for="result-rows">Первые строки вашего реузьтата</label>
                            </div>
                            <table id="result-rows" class="table table-hover table-striped table-fixed">
                                <thead class="thead-dark">
                                <tr>
                                    <th v-for="column in columnNames">{{ column }}</th>
                                </tr>
                                </thead>
                                <tbody>
                                    <tr v-for="row in rows">
                                        <td v-for="column in columnNames">
                                            {{ row[column] || 'NULL' }}
                                        </td>
                                    </tr>
                                </tbody>
                            </table>
                        </div>
                    </form>
                </div>
                <div class="modal-footer">
                    <button type="button" class="btn btn-primary" @click="hide">OK</button>
                </div>
            </div>
        </div>
    </div>
</template>

<script lang="ts" src="./FailureDetailsModal.ts">
</script>
