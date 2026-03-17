import { Pipe, PipeTransform } from '@angular/core';

const TXN_TYPE_LABELS: Record<string, string> = {
  NEW_SUBSCRIPTION: 'Nouvel abonnement',
  RENEWAL: 'Renouvellement',
  SIM_SWAP: 'Échange SIM',
  ACCESSORY_SALE: 'Accessoire',
  TOP_UP: 'Recharge',
  CANCELLATION: 'Résiliation',
};

const TXN_TYPE_ICONS: Record<string, string> = {
  NEW_SUBSCRIPTION: 'add_circle',
  RENEWAL: 'autorenew',
  SIM_SWAP: 'sim_card',
  TOP_UP: 'bolt',
  CANCELLATION: 'cancel',
};

@Pipe({ name: 'txnTypeLabel' })
export class TxnTypeLabelPipe implements PipeTransform {
  transform(type: string | null | undefined): string {
    return TXN_TYPE_LABELS[type || ''] ?? type ?? '';
  }
}

@Pipe({ name: 'txnTypeIcon' })
export class TxnTypeIconPipe implements PipeTransform {
  transform(type: string | null | undefined): string {
    return TXN_TYPE_ICONS[type || ''] ?? 'receipt';
  }
}
