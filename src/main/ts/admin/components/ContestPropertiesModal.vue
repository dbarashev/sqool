<template>
    <div class="modal" tabindex="-1" role="dialog" id="contest-properties">
        <div class="modal-dialog" role="document">
            <div class="modal-content">
                <div class="modal-header">
                    <h5 class="modal-title">Контест {{ contestName }}</h5>
                    <button type="button" class="close" data-dismiss="modal" aria-label="Close">
                        <span aria-hidden="true">&times;</span>
                    </button>
                </div>
                <div class="modal-body">
                    <ul class="nav nav-tabs" id="contestTabs" role="tablist">
                        <li class="nav-item">
                            <a class="nav-link active" id="main-properties-tab" data-toggle="tab" href="#contest-main-properties" role="tab"
                               aria-controls="contest-main-properties" aria-selected="true">Основные свойства</a>
                        </li>
                        <li class="nav-item">
                            <a class="nav-link" id="description-tab" data-toggle="tab" href="#variants" role="tab"
                               aria-controls="variants" aria-selected="false">Варианты</a>
                        </li>
                    </ul>
                    <div class="tab-content" id="contestTabsContent">
                        <div class="tab-pane fade show active" id="contest-main-properties" role="tabpanel" aria-labelledby="main-properties-tab">
                            <form>
                                <div class="form-group">
                                    <label for="contest-properties-code">Код контеста</label>
                                    <input type="text" class="form-control"
                                           id="contest-properties-code"
                                           aria-describedby="contest-properties-code-help"
                                           v-model="contestCode">
                                    <small id="contest-properties-code-help"
                                           class="form-text text-muted">Этот код для роботов.</small>
                                </div>
                                <div class="form-group">
                                    <label for="contest-properties-name">Название</label>
                                    <input type="text" class="form-control"
                                           id="contest-properties-name"
                                           aria-describedby="contest-properties-name-help"
                                           v-model="contestName"
                                    >
                                    <small id="contest-properties-name-help"
                                           class="form-text text-muted">Это название для людей</small>
                                </div>
                                <div class="form-group">
                                    <label for="contest-properties-start">Дата и время начала</label>
                                    <input type="text" class="form-control"
                                           id="contest-properties-start"
                                           aria-describedby="contest-properties-start-help"
                                           v-model="contestStart"
                                    >
                                    <small id="contest-properties-start-help"
                                           class="form-text text-muted">В формате YYYY-MM-DD HH:mm</small>
                                </div>
                                <div class="form-group">
                                    <label for="contest-properties-end">Дата и время окончания</label>
                                    <input type="text" class="form-control"
                                           id="contest-properties-end"
                                           aria-describedby="contest-properties-end-help"
                                           v-model="contestEnd"
                                    >
                                    <small id="contest-properties-end-help"
                                           class="form-text text-muted">В формате YYYY-MM-DD HH:mm</small>
                                </div>
                            </form>
                        </div>
                        <div class="tab-pane fade" id="variants" role="tabpanel" aria-labelledby="variants-tab">
                            <table class="table table-hover table-striped">
                                <tbody>
                                    <tr v-for="variant in variants">
                                        <td style="width: 15%">
                                            <input type="checkbox" :value="variant" v-model="selectedVariants">
                                        </td>
                                        <td class="text-left">{{ variant.name }}</td>
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

<script lang="ts" src="./ContestPropertiesModal.ts">
</script>
