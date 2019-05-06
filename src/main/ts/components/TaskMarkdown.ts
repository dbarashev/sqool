import {Component, Vue} from 'vue-property-decorator';
import Showdown from 'showdown';

@Component
export default class TaskMarkdown extends Vue {
    private text = '';
    private converter = new Showdown.Converter();

    get textValue(): string {
        return this.text;
    }

    set textValue(value: string) {
        this.text = value;
    }

    get htmlValue(): string {
        return this.converter.makeHtml(this.text);
    }

    public markdownText(): string {
        return this.htmlValue;
    }
}
