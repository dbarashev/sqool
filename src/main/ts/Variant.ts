export class VariantDto {
    public active = false;

    constructor(readonly id: number, readonly name: string, readonly tasks: number[]) {}
}
