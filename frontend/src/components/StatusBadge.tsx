import type { VacationStatus } from '../types';

const LABELS: Record<VacationStatus, string> = {
  PENDENTE: 'Pendente',
  APROVADO: 'Aprovado',
  REJEITADO: 'Rejeitado',
};

export function StatusBadge({ status }: { status: VacationStatus }) {
  return <span className={`badge badge-${status.toLowerCase()}`}>{LABELS[status]}</span>;
}
