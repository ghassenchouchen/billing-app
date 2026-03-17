import { StatusLabelPipe, StatusClassPipe } from './status-label.pipe';
import { RoleLabelPipe } from './role-label.pipe';
import { CurrencyDtPipe } from './format-currency.pipe';
import { DateFrPipe } from './format-date-fr.pipe';
import { SimStatusLabelPipe, SimStatusClassPipe } from './sim-status.pipe';
import { TxnTypeLabelPipe, TxnTypeIconPipe } from './txn-type.pipe';

export {
  StatusLabelPipe, StatusClassPipe,
  RoleLabelPipe,
  CurrencyDtPipe,
  DateFrPipe,
  SimStatusLabelPipe, SimStatusClassPipe,
  TxnTypeLabelPipe, TxnTypeIconPipe,
};

export const SHARED_PIPES = [
  StatusLabelPipe,
  StatusClassPipe,
  RoleLabelPipe,
  CurrencyDtPipe,
  DateFrPipe,
  SimStatusLabelPipe,
  SimStatusClassPipe,
  TxnTypeLabelPipe,
  TxnTypeIconPipe,
];
