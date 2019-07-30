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
    private deferred: JQueryDeferred<ContestDto> = $.Deferred<ContestDto>();

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
        this.contestCode = this.contestCode.toLowerCase();
        if (!/^[a-z0-9]+([-_][a-z0-9]+)*$/.test(this.contestCode)) {
            const message = 'Код может содержать слова, состоящие из цифр и букв английского алфавита, разделённые подчеркиванием или дешем';
            this.alertDialog().show('Недопустимый код конеста', message);
            return;
        }
        const selectedVariants = this.selectedVariants.map(variant => variant.id);
        this.deferred.resolve(
            new ContestDto(this.contestCode, this.contestName, this.contestStart, this.contestEnd, selectedVariants)
        );

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
        }).fail(xhr => {
            const title = 'Не удалось получить список вариантов:';
            this.alertDialog().show(title, xhr.statusText);
        });
    }

}
