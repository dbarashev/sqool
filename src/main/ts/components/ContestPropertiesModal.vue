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
                <div class="modal-footer">
                    <button type="button" class="btn btn-primary" @click="submit">OK</button>
                </div>
            </div>
        </div>
    </div>
</template>

<script lang="ts">
import { Component, Vue } from 'vue-property-decorator';
import {ContestDto} from '../Contest';

@Component
export default class ContestPropertiesModal extends Vue {
    public contestName: string = '';
    public contestCode: string = '';
    public contestStart: string = '';
    public contestEnd: string = '';

    private deferred: JQueryDeferred<ContestDto> | undefined;

    public show(contest: ContestDto): JQueryDeferred<ContestDto> {
        $('#contest-properties').modal();
        this.contestName = contest.name;
        this.contestCode = contest.code;
        this.contestStart = contest.start_ts;
        this.contestEnd = contest.end_ts;

        this.deferred = $.Deferred<ContestDto>();
        return this.deferred;
    }

    public hide() {
        $('#contest-properties').modal('hide');
    }

    public submit() {
        if (this.deferred) {
            this.deferred.resolve(new ContestDto(
                this.contestCode, this.contestName, this.contestStart, this.contestEnd));
        }
    }
}
</script>
