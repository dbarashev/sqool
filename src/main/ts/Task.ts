export class ColumnSpec {
    constructor(readonly name: string, readonly type: string) {}
}

export class TaskDto  {
    constructor(readonly id: number,
                readonly name: string,
                readonly description: string,
                // tslint:disable-next-line
                readonly result_json: string) {
    }
}
