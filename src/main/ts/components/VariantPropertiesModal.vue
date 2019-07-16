<template>
    <div class="modal" tabindex="-1" role="dialog" id="variant-properties">
        <div class="modal-dialog w-75 mw-100" role="document">
            <div class="modal-content">
                <div class="modal-header">
                    <h5 class="modal-title">Вариант {{ variantName }}</h5>
                    <button type="button" class="close" data-dismiss="modal" aria-label="Close">
                        <span aria-hidden="true">&times;</span>
                    </button>
                </div>
                <div class="modal-body">
                    <ul class="nav nav-tabs" id="variantTabs" role="tablist">
                        <li class="nav-item">
                            <a class="nav-link active" id="main-properties-tab" data-toggle="tab"
                               href="#variant-main-properties" role="tab"
                               aria-controls="variant-main-properties" aria-selected="true">Основные свойства</a>
                        </li>
                        <li class="nav-item">
                            <a class="nav-link" id="tasks-tab" data-toggle="tab" href="#tasks" role="tab"
                               aria-controls="variant-description" aria-selected="false">Задачи</a>
                        </li>
                    </ul>
                    <div class="tab-content" id="variantTabsContent">
                        <div class="tab-pane fade show active" id="variant-main-properties" role="tabpanel"
                             aria-labelledby="main-properties-tab">
                            <form>
                                <div class="form-group">
                                    <label for="task-properties-name">Название варианта</label>
                                    <input type="text" class="form-control" id="task-properties-name"
                                           aria-describedby="task-properties-name-help"
                                           v-model="variantName">
                                </div>
                            </form>
                        </div>
                        <div class="tab-pane fade" id="tasks" role="tabpanel" aria-labelledby="tasks-tab">
                            <table class="table table-hover">
                                <thead class="thead-dark">
                                <tr>
                                    <th scope="col"><input type="checkbox"></th>
                                    <th scope="col">Задача</th>
                                    <th scope="col">Описание</th>
                                    <th scope="col">Столбцы результата</th>
                                </tr>
                                </thead>
                                <tbody>
                                <tr v-for="t in tasks">
                                    <td><input type="checkbox" :value="t" v-model="selectedTasks"></td>
                                    <td>{{ t.name }}</td>
                                    <td v-html="t.description"></td>
                                    <td>{{ taskResultSpec(t) }}</td>
                                </tr>
                                </tbody>
                            </table>
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

<script lang="ts" src="./VariantPropertiesModal.ts">
</script>
