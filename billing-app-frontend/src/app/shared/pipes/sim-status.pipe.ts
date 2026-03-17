import { Pipe, PipeTransform } from '@angular/core';

const SIM_STATUS_LABELS: Record<string, string> = {
  AVAILABLE: 'Disponible',
  ASSIGNED: 'Attribuée',
  ACTIVATED: 'Activée',
  DAMAGED: 'Endommagée',
  LOST: 'Perdue',
};

const SIM_STATUS_CLASSES: Record<string, string> = {
  AVAILABLE: 'sim-available',
  ASSIGNED: 'sim-assigned',
  ACTIVATED: 'sim-activated',
  DAMAGED: 'sim-damaged',
  LOST: 'sim-lost',
};

@Pipe({ name: 'simStatusLabel' })
export class SimStatusLabelPipe implements PipeTransform {
  transform(status: string | null | undefined): string {
    return SIM_STATUS_LABELS[status || ''] ?? status ?? '';
  }
}

@Pipe({ name: 'simStatusClass' })
export class SimStatusClassPipe implements PipeTransform {
  transform(status: string | null | undefined): string {
    return SIM_STATUS_CLASSES[status || ''] ?? '';
  }
}
