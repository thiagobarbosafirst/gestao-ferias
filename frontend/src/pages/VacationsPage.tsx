import { useCallback, useEffect, useState, type FormEvent } from 'react';
import { usersApi, vacationsApi, type VacationFilters } from '../api';
import { useAuth } from '../auth';
import { Alert } from '../components/Alert';
import { Modal } from '../components/Modal';
import { Pagination } from '../components/Pagination';
import { StatusBadge } from '../components/StatusBadge';
import { formatDate, inclusiveDays, todayIso } from '../dates';
import type { Page, User, VacationRequest } from '../types';

const PAGE_SIZE = 10;

/**
 * Listagem de pedidos de férias com filtros, paginação e ações.
 * O backend já só devolve os pedidos que o utilizador pode ver; aqui decidimos apenas
 * que botões mostrar (o backend volta a validar todas as permissões).
 */
export function VacationsPage() {
  const { user } = useAuth();
  const [filters, setFilters] = useState<VacationFilters>({ status: '', employeeName: '', from: '', to: '' });
  const [pageIndex, setPageIndex] = useState(0);
  const [page, setPage] = useState<Page<VacationRequest> | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);
  const [editing, setEditing] = useState<VacationRequest | 'new' | null>(null);
  const [rejecting, setRejecting] = useState<VacationRequest | null>(null);

  const load = useCallback(() => {
    vacationsApi
      .list({ ...filters, page: pageIndex, size: PAGE_SIZE })
      .then((p) => { setPage(p); setError(null); })
      .catch((e: Error) => setError(e.message));
  }, [filters, pageIndex]);

  useEffect(load, [load]);

  if (!user) return null;
  const isAdmin = user.role === 'ADMIN';
  const seesOthers = user.role !== 'COLLABORATOR';

  /** Aprovar/rejeitar: ADMIN, ou MANAGER para pedidos que não são dele (logo, da sua equipa). */
  const canDecide = (v: VacationRequest) =>
    v.status === 'PENDENTE' && (isAdmin || (user.role === 'MANAGER' && v.employee.id !== user.id));

  /** Executa uma ação, mostra o resultado e recarrega a lista. */
  const run = async (action: () => Promise<unknown>, message: string) => {
    setError(null);
    setSuccess(null);
    try {
      await action();
      setSuccess(message);
      load();
    } catch (e) {
      setError((e as Error).message);
    }
  };

  const updateFilter = (patch: Partial<VacationFilters>) => {
    setFilters((f) => ({ ...f, ...patch }));
    setPageIndex(0);
  };

  return (
    <section>
      <div className="page-header">
        <div>
          <h1>Pedidos de férias</h1>
          <p className="muted">
            {isAdmin ? 'Todos os pedidos da empresa.' : seesOthers ? 'Os seus pedidos e os da sua equipa.' : 'Os seus pedidos.'}
          </p>
        </div>
        <button className="btn btn-primary" onClick={() => setEditing('new')}>+ Novo pedido</button>
      </div>

      <div className="filters card">
        <label>
          Estado
          <select value={filters.status} onChange={(e) => updateFilter({ status: e.target.value as VacationFilters['status'] })}>
            <option value="">Todos</option>
            <option value="PENDENTE">Pendente</option>
            <option value="APROVADO">Aprovado</option>
            <option value="REJEITADO">Rejeitado</option>
          </select>
        </label>
        {seesOthers && (
          <label>
            Colaborador
            <input placeholder="Nome…" value={filters.employeeName}
                   onChange={(e) => updateFilter({ employeeName: e.target.value })} />
          </label>
        )}
        <label>
          De
          <input type="date" value={filters.from} onChange={(e) => updateFilter({ from: e.target.value })} />
        </label>
        <label>
          Até
          <input type="date" value={filters.to} onChange={(e) => updateFilter({ to: e.target.value })} />
        </label>
      </div>

      <Alert message={error} />
      <Alert message={success} kind="success" />

      <div className="card table-wrap">
        <table>
          <thead>
            <tr>
              {seesOthers && <th>Colaborador</th>}
              <th>Período</th>
              <th>Dias</th>
              <th>Estado</th>
              <th>Observações</th>
              <th>Decisão</th>
              <th className="actions-col">Ações</th>
            </tr>
          </thead>
          <tbody>
            {page?.content.map((v) => (
              <tr key={v.id}>
                {seesOthers && <td>{v.employee.name}</td>}
                <td className="nowrap">{formatDate(v.startDate)} → {formatDate(v.endDate)}</td>
                <td>{v.days}</td>
                <td><StatusBadge status={v.status} /></td>
                <td className="muted">{v.notes ?? '—'}</td>
                <td className="muted small">
                  {v.decidedBy ? `por ${v.decidedBy.name}` : '—'}
                  {v.rejectionReason && <div>“{v.rejectionReason}”</div>}
                </td>
                <td className="actions">
                  {canDecide(v) && (
                    <>
                      <button className="btn btn-success" onClick={() => run(() => vacationsApi.approve(v.id), 'Pedido aprovado.')}>Aprovar</button>
                      <button className="btn btn-danger" onClick={() => setRejecting(v)}>Rejeitar</button>
                    </>
                  )}
                  {v.status === 'PENDENTE' && (
                    <button className="btn" onClick={() => setEditing(v)}>Editar</button>
                  )}
                  {v.status !== 'REJEITADO' && (
                    <button className="btn" onClick={() => {
                      if (confirm(`Cancelar o pedido de ${formatDate(v.startDate)} a ${formatDate(v.endDate)}?`)) {
                        run(() => vacationsApi.cancel(v.id), 'Pedido cancelado.');
                      }
                    }}>Cancelar</button>
                  )}
                </td>
              </tr>
            ))}
            {page && page.content.length === 0 && (
              <tr><td colSpan={7} className="empty">Nenhum pedido encontrado.</td></tr>
            )}
          </tbody>
        </table>
        {page && <Pagination page={page} onChange={setPageIndex} />}
      </div>

      {editing && (
        <VacationForm
          request={editing === 'new' ? null : editing}
          canChooseEmployee={isAdmin}
          onClose={() => setEditing(null)}
          onSaved={(msg) => { setEditing(null); setSuccess(msg); setError(null); load(); }}
        />
      )}
      {rejecting && (
        <RejectForm
          request={rejecting}
          onClose={() => setRejecting(null)}
          onDone={() => { setRejecting(null); setSuccess('Pedido rejeitado.'); setError(null); load(); }}
        />
      )}
    </section>
  );
}

/** Formulário de criação/edição de um pedido. */
function VacationForm({ request, canChooseEmployee, onClose, onSaved }: {
  request: VacationRequest | null;
  canChooseEmployee: boolean;
  onClose: () => void;
  onSaved: (message: string) => void;
}) {
  const [startDate, setStartDate] = useState(request?.startDate ?? '');
  const [endDate, setEndDate] = useState(request?.endDate ?? '');
  const [notes, setNotes] = useState(request?.notes ?? '');
  const [userId, setUserId] = useState<number | ''>('');
  const [users, setUsers] = useState<User[]>([]);
  const [error, setError] = useState<string | null>(null);

  // ADMIN pode criar pedidos em nome de qualquer colaborador.
  useEffect(() => {
    if (canChooseEmployee && !request) {
      usersApi.list({ size: 100 }).then((p) => setUsers(p.content)).catch(() => setUsers([]));
    }
  }, [canChooseEmployee, request]);

  const days = startDate && endDate && endDate >= startDate ? inclusiveDays(startDate, endDate) : null;

  const submit = async (e: FormEvent) => {
    e.preventDefault();
    setError(null);
    try {
      const payload = { startDate, endDate, notes, userId: userId === '' ? null : userId };
      if (request) {
        await vacationsApi.update(request.id, payload);
        onSaved('Pedido atualizado.');
      } else {
        await vacationsApi.create(payload);
        onSaved('Pedido criado. Aguarda aprovação.');
      }
    } catch (err) {
      setError((err as Error).message);
    }
  };

  return (
    <Modal title={request ? 'Editar pedido' : 'Novo pedido de férias'} onClose={onClose}>
      <form className="form" onSubmit={submit}>
        <Alert message={error} />
        {canChooseEmployee && !request && (
          <label>
            Colaborador
            <select value={userId} onChange={(e) => setUserId(e.target.value ? Number(e.target.value) : '')}>
              <option value="">Eu próprio</option>
              {users.map((u) => <option key={u.id} value={u.id}>{u.name}</option>)}
            </select>
          </label>
        )}
        <div className="grid-2">
          <label>
            Início
            <input type="date" min={todayIso()} value={startDate} onChange={(e) => setStartDate(e.target.value)} required />
          </label>
          <label>
            Fim
            <input type="date" min={startDate || todayIso()} value={endDate} onChange={(e) => setEndDate(e.target.value)} required />
          </label>
        </div>
        {days !== null && <p className="muted small">{days} dia{days === 1 ? '' : 's'} (início e fim incluídos)</p>}
        <label>
          Observações
          <textarea rows={3} maxLength={500} value={notes} onChange={(e) => setNotes(e.target.value)} />
        </label>
        <div className="form-actions">
          <button type="button" className="btn" onClick={onClose}>Voltar</button>
          <button type="submit" className="btn btn-primary">Guardar</button>
        </div>
      </form>
    </Modal>
  );
}

/** Pede o motivo (opcional) antes de rejeitar. */
function RejectForm({ request, onClose, onDone }: { request: VacationRequest; onClose: () => void; onDone: () => void }) {
  const [reason, setReason] = useState('');
  const [error, setError] = useState<string | null>(null);

  const submit = async (e: FormEvent) => {
    e.preventDefault();
    try {
      await vacationsApi.reject(request.id, reason);
      onDone();
    } catch (err) {
      setError((err as Error).message);
    }
  };

  return (
    <Modal title="Rejeitar pedido" onClose={onClose}>
      <form className="form" onSubmit={submit}>
        <p>
          {request.employee.name}: {formatDate(request.startDate)} → {formatDate(request.endDate)} ({request.days} dias)
        </p>
        <Alert message={error} />
        <label>
          Motivo (opcional)
          <textarea rows={3} maxLength={500} value={reason} onChange={(e) => setReason(e.target.value)} />
        </label>
        <div className="form-actions">
          <button type="button" className="btn" onClick={onClose}>Voltar</button>
          <button type="submit" className="btn btn-danger">Rejeitar</button>
        </div>
      </form>
    </Modal>
  );
}
