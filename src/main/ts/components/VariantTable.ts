import {Component, Vue} from 'vue-property-decorator';
import {VariantDto} from "../Variant";

@Component
export default class VariantTable extends Vue {
    public variants: VariantDto[] = [];
    private activeVariant?: VariantDto;

    public mounted() {
        this.refresh();
    }

    public refresh() {
        $.ajax({
            url: '/admin/variant/all',
        }).done((variants: VariantDto[]) => {
            this.variants = [];
            variants.forEach(variant => this.variants.push(variant));
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