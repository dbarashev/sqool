import {Component, Vue} from 'vue-property-decorator';
import Showdown from 'showdown';

@Component
export default class TaskMarkdown extends Vue {
    private text = '';
    private converter = new Showdown.Converter();

    public markdownText(): string {
        return this.converter.makeHtml(this.text);
    }

    public setText(text: string): void {
        this.text = text;
    }
}
