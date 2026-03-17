import { Pipe, PipeTransform } from '@angular/core';

const ROLE_LABELS: Record<string, string> = {
  ADMIN: 'Administrateur',
  RESPONSABLE_BOUTIQUE: 'Responsable',
  AGENT_COMMERCIAL: 'Agent Commercial',
};

@Pipe({ name: 'roleLabel' })
export class RoleLabelPipe implements PipeTransform {
  transform(role: string | null | undefined): string {
    return ROLE_LABELS[role || ''] ?? role ?? '';
  }
}
