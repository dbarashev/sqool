export class TaskAttempt {
  constructor(
      readonly attemptId: string | null,
      readonly taskEntity: Task,
      readonly count: number,
      readonly status: string | null,
      readonly errorMsg: string | null,
      readonly resultSet: string | null,
  ) {}
}

export class Task {
  constructor(
      readonly id: number,
      readonly name: string,
      readonly signatureJson: string | null,
      readonly description: string | null,
      readonly score: number,
      readonly difficulty: number,
      readonly schemaId: number
  ) {}
}

export function getTaskSignature(task: Task): string {
  if (task.signatureJson === null || task.signatureJson.trim() === '') {
    return '';
  }
  const columns: ColumnSpec[] = JSON.parse(task.signatureJson);
  columns.sort((left: ColumnSpec, right: ColumnSpec): number => parseInt(left.num, 10) - parseInt(right.num, 10));
  return columns.map((column: ColumnSpec) => `${column.name} ${column.type}`).join(', ');
}

class ColumnSpec {
  constructor(readonly name: string, readonly type: string, readonly num: string) {}
}

