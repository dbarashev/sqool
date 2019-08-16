export class TaskAttempt {
  constructor(
      readonly taskEntity: Task,
      readonly count: number,
      readonly status: string | null,
      readonly errorMsg: string | null,
      readonly resultSet: string | null
  ) {}
}

export class Task {
  constructor(
      readonly id: number,
      readonly name: string,
      readonly signatureJson: string | null,
      readonly description: string | null,
      readonly score: number,
      readonly difficulty: number
  ) {}
}

export function getTaskSignature(task: Task): string {
  return (task.signatureJson === null || task.signatureJson.trim() === '')
      ? ''
      : JSON.parse(task.signatureJson).map((column: ColumnSpec) => `${column.name} ${column.type}`).join(', ');
}

class ColumnSpec {
  constructor(readonly name: string, readonly type: string) {}
}

