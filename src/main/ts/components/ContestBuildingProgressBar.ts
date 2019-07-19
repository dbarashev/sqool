import { Component, Vue } from 'vue-property-decorator';

@Component
export default class ContestBuildingProgressBar extends Vue {
    public show() {
        $('#progress-bar').modal();
    }

    public hide() {
        $('#progress-bar').modal('hide');
    }
}
