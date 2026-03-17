import { Pipe, PipeTransform } from '@angular/core';

@Pipe({ name: 'dateFr' })
export class DateFrPipe implements PipeTransform {
  transform(dateStr: string | null | undefined, includeTime = false): string {
    if (!dateStr) return '—';
    try {
      const d = new Date(dateStr);
      const date = d.toLocaleDateString('fr-TN', { day: '2-digit', month: 'short', year: 'numeric' });
      if (!includeTime) return date;
      const time = d.toLocaleTimeString('fr-TN', { hour: '2-digit', minute: '2-digit' });
      return `${date} ${time}`;
    } catch {
      return '—';
    }
  }
}
