<template>
    <div class="modal" tabindex="-1" role="dialog" id="contest-attempts">
        <div class="modal-dialog w-75 mw-100" role="document">
            <div class="modal-content">
                <div class="modal-header">
                    <h5 class="modal-title">Решения задач</h5>
                    <button type="button" class="close" data-dismiss="modal" aria-label="Close">
                        <span aria-hidden="true">&times;</span>
                    </button>
                </div>
                <div class="modal-body">
                    <ul class="nav nav-tabs" role="tablist">
                        <li class="nav-item">
                            <a class="nav-link active"
                               data-toggle="tab"
                               href="#attempts-by-task"
                               role="tab"
                               aria-controls="variant-main-properties" aria-selected="true">По задачам</a>
                        </li>
                        <li class="nav-item">
                            <a class="nav-link"
                               data-toggle="tab"
                               href="#attempts-by-user"
                               role="tab"
                               aria-controls="variant-description" aria-selected="false">По студентам</a>
                        </li>
                    </ul>
                    <div class="tab-content">
                        <div class="tab-pane fade show active" id="attempts-by-task" role="tabpanel">
                            <table class="table table-hover">
                                <thead class="thead-dark">
                                <tr>
                                    <th scope="col"></th>
                                </tr>
                                </thead>
                                <tbody>
                                </tbody>
                            </table>
                        </div>
                        <div class="tab-pane fade" id="attempts-by-user" role="tabpanel">
                            <table class="table table-hover">
                                <thead class="thead-dark">
                                <tr>
                                    <th scope="col"></th>
                                </tr>
                                </thead>
                                <tbody>
                                </tbody>
                            </table>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    </div>
</template>

<script lang="ts" src="./AttemptsModal.ts">
</script>

