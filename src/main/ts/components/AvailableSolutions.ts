import {Component, Inject, Vue} from 'vue-property-decorator';

@Component
export default class AvailableSolutions extends Vue {

    public show() {
        $('#available-solutions').show();
    }

    public hide() {
        $('#available-solutions').hide();
    }
}


