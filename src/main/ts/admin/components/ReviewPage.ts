import {Component, Inject, Vue} from 'vue-property-decorator';
import TaskMarkdown from './TaskMarkdown';
import AlertDialog from '../../components/AlertDialog';
import {ContestDto} from '../Contest';
import ContestMainWindow from './ContestMainWindow';
import {Attempt} from './AttemptTable';
import {TaskDto} from '../Task';

interface Review {
  review_text: string;
  reviewer_name: string;
}

@Component({
  components: {
    TaskMarkdown,
  },
})
export default class ReviewPage extends Vue {

  private get markdown(): TaskMarkdown {
    return this.$refs.taskMarkdown as TaskMarkdown;
  }

  get allReviewsHtml(): string {
    return this.allReviews.map((review) =>
        `
        _${review.reviewer_name}_

        ${TaskMarkdown.markdown2html(review.review_text)}
        `,
    ).join('<br>');
  }
  public attemptUserName: string = '';
  public attemptTaskName: string = '';
  public taskDescriptionHtml: string = '';

  private attempt!: Attempt;
  @Inject() private readonly alertDialog!: () => AlertDialog;
  @Inject() private readonly contestMainWindow!: () => ContestMainWindow;
  private contest?: ContestDto;
  private allReviews: Review[] = [];

  public getTaskDescriptionHtml() {
    if (this.taskDescriptionHtml === '') {
      $.ajax({
        url: '/admin/task/all',
        data: {
          id: this.attempt.task_id,
        },
      }).done((task: TaskDto) => {
        this.taskDescriptionHtml = TaskMarkdown.markdown2html(task.description);
      }).fail((xhr) => {
        const title = 'Не удалось получить условие задачи:';
        this.alertDialog().show(title, xhr.statusText);
      });
    }
  }

  public getAttempt() {
    $.ajax({
      url: '/admin/submission/get',
      method: 'GET',
      data: {
        attempt_id: this.attempt.attempt_id,
      },
    }).done((attempt) => {
      this.markdown.textValue = attempt.attempt_text;
    }).fail((xhr) => {
      const title = 'Не загрузить решение:';
      this.alertDialog().show(title, xhr.statusText);
    });
  }

  public getLastReview() {
    $.ajax({
      url: '/admin/review/get',
      method: 'GET',
      data: {
        attempt_id: this.attempt.attempt_id,
      },
    }).done((review: Review) => {
      this.markdown.textValue = review.review_text;
    }).fail((xhr) => {
      const title = 'Не удалось загрузить рецензию:';
      this.alertDialog().show(title, xhr.statusText);
    });
  }

  public save() {
    $.ajax({
      url: '/admin/review/save',
      method: 'POST',
      data: {
        attempt_id: this.attempt.attempt_id,
        solution_review: this.markdown.textValue,
      },
    }).fail((xhr) => {
      const title = 'Не удалось сохранить рецензию:';
      this.alertDialog().show(title, xhr.statusText);
    });
  }

  public hide() {
    $(this.$el).hide(); // .setAttribute('hidden', 'true');
  }

  public show(contest: ContestDto, attempt: Attempt) {
    this.contest = contest;
    this.attempt = attempt;
    this.attemptUserName = attempt.user_name;
    this.attemptTaskName = attempt.name;
    this.taskDescriptionHtml = '';
    this.getLastReview();
    this.getAllReviews();
    this.getTaskDescriptionHtml();
    $(this.$el).show(); // .removeAttribute('hidden');
  }

  public backToStudents() {
    this.contestMainWindow().showAttemptTableByStudent(this.contest!!);
  }

  public getAllReviews() {
    if (!this.attempt) {
      return;
    }
    $.ajax({
      url: '/admin/review/list',
      method: 'GET',
      data: {
        attempt_id: this.attempt.attempt_id,
      },
    }).done((reviews: Review[]) => {
      this.allReviews = reviews;
    }).fail((xhr) => {
      const title = 'Не удалось загрузить рецензию:';
      this.alertDialog().show(title, xhr.statusText);
    });
  }
}
