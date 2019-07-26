import {Component, Vue} from 'vue-property-decorator';

@Component
export default class AlertDialog extends Vue {
    public title: string = '';
    public message: string = '';

    public show(title: string, message: string = '') {
        this.title = title;
        this.message = message;
        $('#alert-dialog').modal();
    }

    public hide() {
        $('#alert-dialog').modal('hide');
    }
}
