export class Attempt {
  constructor(
      readonly task_id: number,
      readonly variant_id: number,
      readonly name: string,
      readonly attempt_id: number,
      readonly status: string
  ) {}
}
