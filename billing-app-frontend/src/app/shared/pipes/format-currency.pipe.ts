import { Pipe, PipeTransform } from '@angular/core';

@Pipe({ name: 'currencyDt' })
export class CurrencyDtPipe implements PipeTransform {
  transform(amount: number | null | undefined): string {
    return (amount ?? 0).toFixed(2).replace('.', ',') + ' DT';
  }
}
