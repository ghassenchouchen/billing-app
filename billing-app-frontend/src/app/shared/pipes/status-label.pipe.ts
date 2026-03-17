import { Pipe, PipeTransform } from '@angular/core';

const STATUS_LABELS: Record<string, string> = {
  ACTIVE: 'Actif',
  SUSPENDED: 'Suspendu',
  INACTIVE: 'Inactif',
  DISABLED: 'Inactif',
  CLOSED: 'Fermée',
  TERMINATED: 'Résilié',
  CANCELLED: 'Annulé',
  PENDING: 'En attente',
  COMPLETED: 'Terminé',
};

const STATUS_CLASSES: Record<string, string> = {
  ACTIVE: 'status-active',
  SUSPENDED: 'status-inactive',
  INACTIVE: 'status-inactive',
  DISABLED: 'status-inactive',
  CLOSED: 'status-closed',
  TERMINATED: 'status-closed',
  CANCELLED: 'status-closed',
  PENDING: 'txn-pending',
  COMPLETED: 'txn-completed',
};

@Pipe({ name: 'statusLabel' })
export class StatusLabelPipe implements PipeTransform {
  transform(status: string | null | undefined): string {
    return STATUS_LABELS[status || ''] ?? status ?? 'N/A';
  }
}

@Pipe({ name: 'statusClass' })
export class StatusClassPipe implements PipeTransform {
  transform(status: string | null | undefined): string {
    return STATUS_CLASSES[status || ''] ?? '';
  }
}
