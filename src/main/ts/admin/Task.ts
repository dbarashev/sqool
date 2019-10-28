export class ColumnSpec {
  constructor(readonly name: string, readonly type: string, readonly num: string) {}
}

export function getTaskResultSql(task: TaskDto): string {
  return (task.result_json.trim() === '')
      ? ''
      : (JSON.parse(task.result_json) as ColumnSpec[])
          .sort((a, b) => parseInt(a.num) - parseInt(b.num))
          .map((column: ColumnSpec) => `${column.name} ${column.type}`)
          .join(',');
}

export class TaskDto {
  public active = false;

  constructor(
      readonly id: number,
      readonly name: string,
      readonly description: string,
      // tslint:disable-next-line
      readonly has_result: boolean,
      // tslint:disable-next-line
      readonly result_json: string,
      readonly solution: string,
      readonly script_id: number | null,
  ) {}
}
