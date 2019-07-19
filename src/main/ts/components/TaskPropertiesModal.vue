<template>
    <div class="modal" tabindex="-1" role="dialog" id="task-properties">
        <div class="modal-dialog w-75 mw-100" role="document">
            <div class="modal-content">
                <div class="modal-header">
                    <h5 class="modal-title">Задача {{ taskName }}</h5>
                    <button type="button" class="close" data-dismiss="modal" aria-label="Close">
                        <span aria-hidden="true">&times;</span>
                    </button>
                </div>
                <div class="modal-body">
                    <ul class="nav nav-tabs" id="taskTabs" role="tablist">
                        <li class="nav-item">
                            <a class="nav-link active" id="main-properties-tab" data-toggle="tab" href="#task-main-properties" role="tab"
                               aria-controls="task-main-properties" aria-selected="true">Основные свойства</a>
                        </li>
                        <li class="nav-item">
                            <a class="nav-link" id="description-tab" data-toggle="tab" href="#task-description" role="tab"
                               aria-controls="task-description" aria-selected="false">Описание</a>
                        </li>
                        <li class="nav-item">
                            <a class="nav-link" id="solution-tab" data-toggle="tab" href="#task-solution" role="tab"
                               aria-controls="task-solution" aria-selected="false">Решение</a>
                        </li>
                    </ul>
                    <div class="tab-content" id="taskTabsContent">
                        <div class="tab-pane fade show active" id="task-main-properties" role="tabpanel" aria-labelledby="main-properties-tab">
                            <form>
                                <fieldset class="form-group">
                                    <div class="row">
                                        <label for="task-properties-name" class="col-form-label col-sm-3">Название задачи</label>
                                        <div class="col-sm-9 text-left">
                                            <input type="text" class="form-control" id="task-properties-name" aria-describedby="task-properties-name-help"
                                                   v-model="taskName">
                                            <small id="task-properties-name-help"
                                                   class="form-text text-muted">Обычно это номер.</small>
                                        </div>
                                    </div>
                                </fieldset>


                                <fieldset class="form-group">
                                    <div class="row">
                                        <label for="task-properties-name"
                                               class="col-form-label col-sm-3">Столбцы результата</label>
                                        <div class="col-sm-9 text-left">
                                            <input type="text" class="form-control"
                                                   id="task-properties-result"
                                                   aria-describedby="task-properties-result-help"
                                                   v-model="taskResult"
                                            >
                                            <small id="task-properties-result-help"
                                                   class="form-text text-muted"><code>id INT, name TEXT, ...</code></small>
                                        </div>
                                    </div>
                                </fieldset>


                                <fieldset class="form-group">
                                    <div class="row">
                                        <legend class="col-form-label col-sm-3">Схема БД</legend>
                                        <div class="col-sm-9 text-left">
                                            <TaskScriptDropdown ref="taskScriptDropdown"></TaskScriptDropdown>
                                            <small id="task-properties-schema-help"
                                                   class="form-text text-muted">Если задача рассчитывает на наличие каких-то таблиц.</small>
                                        </div>
                                    </div>
                                </fieldset>
                            </form>
                        </div>
                        <div class="tab-pane fade" id="task-description" role="tabpanel" aria-labelledby="description-tab">
                            <TaskMarkdown ref="taskMarkdown"> </TaskMarkdown>
                        </div>
                        <div class="tab-pane fade" id="task-solution" role="tabpanel" aria-labelledby="solution-tab">
                            <form>
                                <div class="form-group">
                                    <label for="task-solution-text">Авторское решение</label>
                                    <textarea class="form-control"
                                              v-model="taskSolution"
                                              id="task-solution-text" rows="15">
                                    </textarea>
                                </div>
                            </form>
                        </div>
                    </div>
                </div>
                <div class="modal-footer">
                    <button type="button" class="btn btn-primary" @click="submit">OK</button>
                </div>
            </div>
        </div>
    </div>
</template>

<script lang="ts" src="./TaskPropertiesModal.ts">
</script>

<style scoped lang="scss">
    .tab-pane .row {
        align-items: baseline;
        .col-form-label {
            text-align: right;
        }
    }

</style>

