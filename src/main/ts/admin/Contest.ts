export class ContestDto {
  public active: boolean = false;

  constructor(
      readonly code: string,
      readonly name: string,
      // tslint:disable-next-line
      readonly start_ts: string,
      // tslint:disable-next-line
      readonly end_ts: string,
      readonly variants: number[]
  ) {}
}
