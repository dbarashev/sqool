import {Component, Inject, Vue} from 'vue-property-decorator';
import {VariantDto} from '../Variant';
import VariantPropertiesModal from './VariantPropertiesModal';
import VariantTable from './VariantTable';
import AlertDialog from '../../components/AlertDialog';

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
    @Inject() private readonly alertDialog!: () => AlertDialog;

    public createNewVariant() {
        const newVariant = new VariantDto(-1, '000', []);
        this.showAndSubmitVariant(newVariant, '/admin/variant/new');
    }

    public editVariant() {
        const activeVariant = this.variantTable().getActiveVariant();
        if (activeVariant) {
            this.showAndSubmitVariant(activeVariant, '/admin/variant/update');
        }
    }

    private showAndSubmitVariant(variant: VariantDto, url: string) {
        this.variantProperties().show(variant).then((updatedVariant) => {
            variant = updatedVariant;
            return $.ajax(url, VariantToolbar.buildVariantPayload(updatedVariant));
        }).done(() => {
            this.variantProperties().hide();
            this.variantTable().refresh();
        }).fail(xhr => {
            // Call it again to be able to make another request
            this.showAndSubmitVariant(variant, url);
            const title = `Что-то пошло не так: ${xhr.status}`;
            this.alertDialog().show(title, xhr.statusText);
        });
    }
}
