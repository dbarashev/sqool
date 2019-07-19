import {Component, Inject, Vue} from 'vue-property-decorator';
import {ContestDto} from '../Contest';
import {VariantDto} from "../Variant";
import AlertDialog from "./AlertDialog";

@Component
export default class ContestPropertiesModal extends Vue {
    public contestName: string = '';
    public contestCode: string = '';
    public contestStart: string = '';
    public contestEnd: string = '';
    public variants: VariantDto[] = [];
    public selectedVariants: VariantDto[] = [];

    @Inject() private readonly alertDialog!: () => AlertDialog;
    private deferred: JQueryDeferred<ContestDto> | undefined;

    public show(contest: ContestDto): JQueryDeferred<ContestDto> {
        $('#contest-properties').modal();
        this.contestName = contest.name;
        this.contestCode = contest.code;
        this.contestStart = contest.start_ts;
        this.contestEnd = contest.end_ts;
        this.updateVariants(contest.variants);

        this.deferred = $.Deferred<ContestDto>();
        return this.deferred;
    }

    public hide() {
        $('#contest-properties').modal('hide');
    }

    public submit() {
        if (!/^[a-z0-9]+(\.?_{0,2}-*[a-z0-9]+)*$/.test(this.contestCode)) {
            const message = 'Код должен соответствовать регулярному выржаению:\n[a-z0-9]+(\\.?_{0,2}-*[a-z0-9]+)*';
            this.alertDialog().show('Недопустимый код конеста', message);
            return;
        }
        if (this.deferred) {
            const selectedVariants = this.selectedVariants.map(variant => variant.id);
            this.deferred.resolve(new ContestDto(
                this.contestCode, this.contestName, this.contestStart, this.contestEnd, selectedVariants));
        }
    }

    private updateVariants(selectedVariantIdList: number[]) {
        this.variants = [];
        this.selectedVariants = [];
        $.ajax({
            url: '/admin/variant/all',
        }).done((variants: VariantDto[]) => {
            variants.forEach(variant => {
                this.variants.push(variant);
                if (selectedVariantIdList.includes(variant.id)) {
                    this.selectedVariants.push(variant)
                }
            });
        });
    }

}
