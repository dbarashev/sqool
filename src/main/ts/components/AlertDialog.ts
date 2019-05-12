import {Component, Vue} from 'vue-property-decorator';

@Component
export default class AlertDialog extends Vue {
    public title: String = '';
    public message: String = '';

    public show(title: String, message: String = "") {
        this.title = title;
        this.message = message;
        $('#alert-dialog').modal();
    }

    public hide() {
        $('#alert-dialog').modal("hide");
    }
}