import {Component, Vue} from 'vue-property-decorator';
import {ContestDto} from '../Contest';

@Component
export default class AttemptsModal extends Vue {
  public show(contest: ContestDto) {
    $('#contest-attempts').modal();
  }
}
