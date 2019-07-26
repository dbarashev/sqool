import {Component, Inject, Vue} from 'vue-property-decorator';
import {VariantDto} from '../Variant';
import AlertDialog from './AlertDialog';

@Component
export default class VariantTable extends Vue {
    public variants: VariantDto[] = [];
    private activeVariant?: VariantDto;
    @Inject() private readonly alertDialog!: () => AlertDialog;

    public mounted() {
        this.refresh();
    }

    public refresh() {
        $.ajax({
            url: '/admin/variant/all',
        }).done((variants: VariantDto[]) => {
            this.variants = [];
            variants.forEach(variant => this.variants.push(variant));
        }).fail(xhr => {
            const title = 'Не удалось получить список вариантов:';
            this.alertDialog().show(title, xhr.statusText);
        });
    }

    public getActiveVariant(): VariantDto | undefined {
        return this.activeVariant;
    }

    public makeActive(variant: VariantDto) {
        if (this.activeVariant) {
            this.activeVariant.active = false;
        }
        this.activeVariant = variant;
        variant.active = true;
        this.$forceUpdate();
    }
}
