// Utilitários de datas. As datas da API vêm como "yyyy-MM-dd" (sem hora),
// por isso trabalhamos com strings/partes para evitar problemas de fuso horário.

/** Date -> "yyyy-MM-dd" (usando a data local). */
export function toIso(d: Date): string {
  const m = String(d.getMonth() + 1).padStart(2, '0');
  const day = String(d.getDate()).padStart(2, '0');
  return `${d.getFullYear()}-${m}-${day}`;
}

/** "yyyy-MM-dd" -> "dd/MM/yyyy" */
export function formatDate(iso: string): string {
  const [y, m, d] = iso.split('-');
  return `${d}/${m}/${y}`;
}

export function todayIso(): string {
  return toIso(new Date());
}

/** Número de dias entre duas datas, contando as duas (datas inclusivas, RB-002). */
export function inclusiveDays(start: string, end: string): number {
  const ms = Date.parse(end + 'T00:00:00Z') - Date.parse(start + 'T00:00:00Z');
  return Math.round(ms / 86_400_000) + 1;
}
