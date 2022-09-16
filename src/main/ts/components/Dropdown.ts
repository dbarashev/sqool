import {Vue, Component, Prop} from 'vue-property-decorator';

@Component
export default class Dropdown extends Vue {
  @Prop()
  public readonly options!: Option[];
  @Prop()
  public readonly value!: Option;

  get selected(): Option {
    return this.value || this.options[0];
  }

  set selected(value: Option) {
    this.$emit('input', value);
  }

  public mounted() {
    $(this.$refs.dropdown).dropdown();
  }
}

interface Option { value: any; text: string; }
