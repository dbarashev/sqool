import {Component, Vue} from 'vue-property-decorator';
import Showdown from 'showdown';

const converter = new Showdown.Converter();

@Component
export default class TaskMarkdown extends Vue {
  public static markdown2html(markdown: string) {
    return converter.makeHtml(markdown);
  }

  private text = '';

  get textValue(): string {
    return this.text;
  }

  set textValue(value: string) {
    this.text = value;
  }

  get htmlValue(): string {
    return TaskMarkdown.markdown2html(this.text);
  }

  public markdownText(): string {
    return this.htmlValue;
  }
}
