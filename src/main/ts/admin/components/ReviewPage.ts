import {Component, Vue} from 'vue-property-decorator';
import TaskMarkdown from './TaskMarkdown';

@Component({
  components: {
    TaskMarkdown,
  },
})
export default class ReviewPage extends Vue {
  public taskId = -1;
  public userId = -1;

  public getAttempt() {
    const markdown = this.$refs.taskMarkdown as TaskMarkdown;
    $.ajax({
      url: '/admin/submission/get',
      method: 'GET',
      data: {
        task_id: this.taskId,
        user_id: this.userId,
      },
    }).then((attempt) => {
      markdown.textValue = attempt.attempt_text;
    });
  }

  public getLastReview() {
    const markdown = this.$refs.taskMarkdown as TaskMarkdown;
    $.ajax({
      url: '/admin/review/get',
      method: 'GET',
      data: {
        task_id: this.taskId,
        user_id: this.userId,
      },
    }).then((review) => {
      markdown.textValue = review.review_text;
    });
  }

  public save() {
    const markdown = this.$refs.taskMarkdown as TaskMarkdown;
    $.ajax({
      url: '/admin/review/save',
      method: 'POST',
      data: {
        task_id: this.taskId,
        user_id: this.userId,
        solution_review: markdown.textValue,
      },
    });
  }

  public hide() {
    this.$el.setAttribute('hidden', 'true');
  }

  public show(userId: number, taskId: number) {
    this.userId = userId;
    this.taskId = taskId;
    this.$el.removeAttribute('hidden');
  }
}
