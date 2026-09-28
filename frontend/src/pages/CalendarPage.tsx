import { useEffect, useMemo, useState } from 'react';
import { vacationsApi } from '../api';
import { Alert } from '../components/Alert';
import { toIso, todayIso } from '../dates';
import type { CalendarEntry } from '../types';

const WEEKDAYS = ['Seg', 'Ter', 'Qua', 'Qui', 'Sex', 'Sáb', 'Dom'];
const MONTHS = ['Janeiro', 'Fevereiro', 'Março', 'Abril', 'Maio', 'Junho', 'Julho', 'Agosto',
  'Setembro', 'Outubro', 'Novembro', 'Dezembro'];

/**
 * Vista mensal dos dias ocupados.
 * Como não pode haver duas pessoas de férias no mesmo dia, cada dia tem no máximo um pedido.
 * Pedidos de outras pessoas que o utilizador não pode ver aparecem como "Ocupado".
 */
export function CalendarPage() {
  const [month, setMonth] = useState(() => {
    const now = new Date();
    return new Date(now.getFullYear(), now.getMonth(), 1);
  });
  const [entries, setEntries] = useState<CalendarEntry[]>([]);
  const [error, setError] = useState<string | null>(null);

  // Grelha de 6 semanas a começar na segunda-feira anterior (ou igual) ao dia 1.
  const days = useMemo(() => {
    const offset = (month.getDay() + 6) % 7; // getDay(): 0 = domingo -> converter para segunda = 0
    const start = new Date(month.getFullYear(), month.getMonth(), 1 - offset);
    return Array.from({ length: 42 }, (_, i) => new Date(start.getFullYear(), start.getMonth(), start.getDate() + i));
  }, [month]);

  useEffect(() => {
    vacationsApi
      .calendar(toIso(days[0]), toIso(days[days.length - 1]))
      .then((e) => { setEntries(e); setError(null); })
      .catch((e: Error) => setError(e.message));
  }, [days]);

  const entryFor = (iso: string) => entries.find((e) => e.startDate <= iso && iso <= e.endDate);
  const shiftMonth = (delta: number) => setMonth((m) => new Date(m.getFullYear(), m.getMonth() + delta, 1));
  const today = todayIso();

  return (
    <section>
      <div className="page-header">
        <div>
          <h1>Calendário</h1>
          <p className="muted">Dias já reservados (pedidos pendentes e aprovados).</p>
        </div>
        <div className="row">
          <button className="btn" onClick={() => shiftMonth(-1)}>‹</button>
          <strong className="month-label">{MONTHS[month.getMonth()]} {month.getFullYear()}</strong>
          <button className="btn" onClick={() => shiftMonth(1)}>›</button>
        </div>
      </div>

      <Alert message={error} />

      <div className="card">
        <div className="calendar">
          {WEEKDAYS.map((w) => <div key={w} className="cal-weekday">{w}</div>)}
          {days.map((d) => {
            const iso = toIso(d);
            const entry = entryFor(iso);
            const outside = d.getMonth() !== month.getMonth();
            return (
              <div key={iso} className={`cal-day ${outside ? 'outside' : ''} ${iso === today ? 'today' : ''}`}>
                <div className="cal-num">{d.getDate()}</div>
                {entry && (
                  <div className={`cal-chip chip-${entry.status.toLowerCase()} ${entry.visible ? '' : 'chip-hidden'}`}
                       title={`${entry.employeeName ?? 'Ocupado'} · ${entry.status}`}>
                    {entry.employeeName ?? 'Ocupado'}
                  </div>
                )}
              </div>
            );
          })}
        </div>
        <div className="legend">
          <span><i className="dot chip-aprovado" /> Aprovado</span>
          <span><i className="dot chip-pendente" /> Pendente</span>
          <span><i className="dot chip-hidden" /> Ocupado por outro colaborador</span>
        </div>
      </div>
    </section>
  );
}
