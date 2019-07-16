import {Component, Inject, Vue} from 'vue-property-decorator';
import {VariantDto} from '../Variant';
import VariantPropertiesModal from './VariantPropertiesModal';
import VariantTable from './VariantTable'

@Component
export default class VariantToolbar extends Vue {
    private static buildVariantPayload(variant: VariantDto): object {
        return {
            method: 'POST',
            data: {
                id: variant.id,
                name: variant.name,
                tasks: JSON.stringify(variant.tasks)
            },
        };
    }
    @Inject() public readonly variantProperties!: () => VariantPropertiesModal;
    @Inject() public readonly variantTable!: () => VariantTable;

    public createNewVariant() {
        const newVariant = new VariantDto(-1, '000', []);
        this.variantProperties().show(newVariant).then((updatedVariant) => {
            return $.ajax('/admin/variant/new', VariantToolbar.buildVariantPayload(updatedVariant));
        }).then(() => {
            this.variantProperties().hide();
            this.variantTable().refresh();
        });
    }

    public editVariant() {
        const activeVariant = this.variantTable().getActiveVariant();
        if (activeVariant) {
            this.variantProperties().show(activeVariant).then((updatedVariant) => {
                $.ajax('/admin/variant/update', VariantToolbar.buildVariantPayload(updatedVariant));
            }).then(() => {
                this.variantProperties().hide();
                this.variantTable().refresh();
            });
        }
    }
}
