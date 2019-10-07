import {Component, Inject, Vue} from 'vue-property-decorator';
import TaskMarkdown from './TaskMarkdown';
import AlertDialog from '../../components/AlertDialog';
import {ContestDto} from '../Contest';
import ContestMainWindow from './ContestMainWindow';

interface Review {
  review_text: string;
}

@Component({
  components: {
    TaskMarkdown,
  },
})
export default class ReviewPage extends Vue {

  public attemptId = '';
  @Inject() private readonly alertDialog!: () => AlertDialog;
  @Inject() private readonly contestMainWindow!: () => ContestMainWindow;
  private contest?: ContestDto;

  public getAttempt() {
    const markdown = this.$refs.taskMarkdown as TaskMarkdown;
    $.ajax({
      url: '/admin/submission/get',
      method: 'GET',
      data: {
        attempt_id: this.attemptId,
      },
    }).done((attempt) => {
      markdown.textValue = attempt.attempt_text;
    }).fail((xhr) => {
      const title = 'Не загрузить решение:';
      this.alertDialog().show(title, xhr.statusText);
    });
  }

  public getLastReview() {
    const markdown = this.$refs.taskMarkdown as TaskMarkdown;
    $.ajax({
      url: '/admin/review/get',
      method: 'GET',
      data: {
        attempt_id: this.attemptId,
      },
    }).done((review: Review) => {
      markdown.textValue = review.review_text;
    }).fail((xhr) => {
      const title = 'Не удалось загрузить рецензию:';
      this.alertDialog().show(title, xhr.statusText);
    });
  }

  public save() {
    const markdown = this.$refs.taskMarkdown as TaskMarkdown;
    $.ajax({
      url: '/admin/review/save',
      method: 'POST',
      data: {
        attempt_id: this.attemptId,
        solution_review: markdown.textValue,
      },
    }).fail((xhr) => {
      const title = 'Не удалось сохранить рецензию:';
      this.alertDialog().show(title, xhr.statusText);
    });
  }
  public hide() {
    $(this.$el).hide(); // .setAttribute('hidden', 'true');
  }

  public show(contest: ContestDto, attemptId: string) {
    this.contest = contest;
    this.attemptId = attemptId;
    this.getLastReview();
    $(this.$el).show(); // .removeAttribute('hidden');
  }

  private backToStudents() {
    this.contestMainWindow().showAttemptTableByStudent(this.contest!!);
  }
}
