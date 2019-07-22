export class ScriptDto {
    public active = false;

    constructor(readonly id: number, readonly description: string, readonly body: string) {}
}
