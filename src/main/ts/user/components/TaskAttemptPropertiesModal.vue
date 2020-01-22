<template>
    <div class="modal" tabindex="-1" role="dialog" id="task-attempt">
        <div class="modal-dialog" role="document">
            <div class="modal-content">
                <div class="modal-header">
                    <h5 class="modal-title">Задача {{ attempt.taskEntity.name }}</h5>
                    <button type="button" class="close" data-dismiss="modal" aria-label="Close">
                        <span aria-hidden="true">&times;</span>
                    </button>
                </div>
                <div class="modal-body">
                    <ul class="nav nav-tabs" role="tablist">
                        <li class="nav-item">
                            <a class="nav-link active" id="description-tab" data-toggle="tab" href="#task-description" role="tab"
                               aria-controls="contest-main-properties" aria-selected="true">Условие</a>
                        </li>
                        <li class="nav-item">
                            <a class="nav-link" id="solution-tab" data-toggle="tab" href="#solution" role="tab"
                               aria-controls="variants" aria-selected="false">Решение</a>
                        </li>
                        <li class="nav-item">
                            <a class="nav-link" id="review-tab" data-toggle="tab" href="#review" role="tab"
                               aria-controls="variants" aria-selected="false">Рецензия</a>
                        </li>
                    </ul>
                    <div class="tab-content">
                        <div class="tab-pane fade show active" id="task-description" role="tabpanel" aria-labelledby="description-tab">
                            <form>
                                <div class="form-group text-left">
                                    <label class="bmd-label-static" for="task-description-text">Условие</label>
                                    <div id="task-description-text" v-html="markdownText(attempt.taskEntity.description)"></div>
                                </div>
                                <div class="form-group text-left">
                                    <label class="bmd-label-static" for="task-result">Результат</label>
                                    <div id="task-result"><code>{{ taskSignature }}</code></div>
                                </div>
                                <div class="form-group text-left" v-if="hasSchema">
                                    <label class="bmd-label-static" for="task-result">Схема БД</label>
                                    <div id="task-schema"><a v-bind:href="schemaBodyUrl" target="_blank">Открыть схему</a></div>
                                </div>
                            </form>
                        </div>
                        <div class="tab-pane fade" id="solution" role="tabpanel" aria-labelledby="solution-tab">
                            <div class="d-flex justify-content-center align-items-center spinner-glasspane" style="" v-if="isGrading">
                                <i class="fas fa-spinner fa-spin fa-5x" aria-hidden="true"></i>
                            </div>
                            <form>
                                <div class="form-group">
                                    <label for="task-solution-text">Текст решения</label>
                                    <textarea class="form-control" v-model="taskSolution" id="task-solution-text" rows="15">
                                    </textarea>
                                </div>
                            </form>
                        </div>
                        <div class="tab-pane fade" id="review" role="tabpanel" aria-labelledby="review-tab">
                                <div class="form-group">
                                    <!-- TODO: get review from the server -->
                                    <div class="text-left" v-html="markdownText(review)"></div>
                                </div>
                        </div>
                    </div>
                </div>
                <div class="modal-footer">
                    <button type="button" class="btn btn-primary" @click="submit">Отправить</button>
                </div>
            </div>
        </div>
    </div>
</template>

<script lang="ts" src="./TaskAttemptPropertiesModal.ts">
</script>

<style lang="scss">
    .spinner-glasspane {
        opacity: 0.7;
        position: absolute;
        top: 0;
        left: 0;
        right: 0;
        bottom: 0;
        z-index: 1;
        background: whitesmoke;
    }
    .tab-pane {
        position: relative;
    }

</style>


