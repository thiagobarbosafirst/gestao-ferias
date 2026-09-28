import { useCallback, useEffect, useState, type FormEvent } from 'react';
import { usersApi, type UserFilters } from '../api';
import { useAuth } from '../auth';
import { Alert } from '../components/Alert';
import { Modal } from '../components/Modal';
import { Pagination } from '../components/Pagination';
import { ROLE_LABELS, type Page, type Role, type User } from '../types';

const PAGE_SIZE = 10;

/** Gestão de colaboradores (apenas ADMIN). */
export function UsersPage() {
  const { user: me } = useAuth();
  const [filters, setFilters] = useState<UserFilters>({ search: '', role: '' });
  const [pageIndex, setPageIndex] = useState(0);
  const [page, setPage] = useState<Page<User> | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);
  const [editing, setEditing] = useState<User | 'new' | null>(null);

  const load = useCallback(() => {
    usersApi
      .list({ ...filters, page: pageIndex, size: PAGE_SIZE })
      .then((p) => { setPage(p); setError(null); })
      .catch((e: Error) => setError(e.message));
  }, [filters, pageIndex]);

  useEffect(load, [load]);

  const updateFilter = (patch: Partial<UserFilters>) => {
    setFilters((f) => ({ ...f, ...patch }));
    setPageIndex(0);
  };

  const remove = async (u: User) => {
    if (!confirm(`Remover ${u.name}? Os pedidos de férias deste colaborador também serão removidos.`)) return;
    setError(null);
    setSuccess(null);
    try {
      await usersApi.remove(u.id);
      setSuccess(`${u.name} foi removido.`);
      load();
    } catch (e) {
      setError((e as Error).message);
    }
  };

  return (
    <section>
      <div className="page-header">
        <div>
          <h1>Colaboradores</h1>
          <p className="muted">Criar, editar e remover utilizadores. Cada colaborador tem um manager.</p>
        </div>
        <button className="btn btn-primary" onClick={() => setEditing('new')}>+ Novo colaborador</button>
      </div>

      <div className="filters card">
        <label>
          Pesquisa
          <input placeholder="Nome ou email…" value={filters.search} onChange={(e) => updateFilter({ search: e.target.value })} />
        </label>
        <label>
          Role
          <select value={filters.role} onChange={(e) => updateFilter({ role: e.target.value as Role | '' })}>
            <option value="">Todos</option>
            <option value="ADMIN">Admin</option>
            <option value="MANAGER">Manager</option>
            <option value="COLLABORATOR">Colaborador</option>
          </select>
        </label>
      </div>

      <Alert message={error} />
      <Alert message={success} kind="success" />

      <div className="card table-wrap">
        <table>
          <thead>
            <tr>
              <th>Nome</th>
              <th>Email</th>
              <th>Role</th>
              <th>Manager</th>
              <th className="actions-col">Ações</th>
            </tr>
          </thead>
          <tbody>
            {page?.content.map((u) => (
              <tr key={u.id}>
                <td>{u.name}</td>
                <td className="muted">{u.email}</td>
                <td><span className={`role role-${u.role.toLowerCase()}`}>{ROLE_LABELS[u.role]}</span></td>
                <td>{u.manager?.name ?? '—'}</td>
                <td className="actions">
                  <button className="btn" onClick={() => setEditing(u)}>Editar</button>
                  {u.id !== me?.id && <button className="btn btn-danger" onClick={() => remove(u)}>Remover</button>}
                </td>
              </tr>
            ))}
            {page && page.content.length === 0 && (
              <tr><td colSpan={5} className="empty">Nenhum colaborador encontrado.</td></tr>
            )}
          </tbody>
        </table>
        {page && <Pagination page={page} onChange={setPageIndex} />}
      </div>

      {editing && (
        <UserForm
          user={editing === 'new' ? null : editing}
          onClose={() => setEditing(null)}
          onSaved={(msg) => { setEditing(null); setSuccess(msg); setError(null); load(); }}
        />
      )}
    </section>
  );
}

/** Formulário de criação/edição de utilizador. */
function UserForm({ user, onClose, onSaved }: { user: User | null; onClose: () => void; onSaved: (msg: string) => void }) {
  const [name, setName] = useState(user?.name ?? '');
  const [email, setEmail] = useState(user?.email ?? '');
  const [password, setPassword] = useState('');
  const [role, setRole] = useState<Role>(user?.role ?? 'COLLABORATOR');
  const [managerId, setManagerId] = useState<number | ''>(user?.manager?.id ?? '');
  const [managers, setManagers] = useState<User[]>([]);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    usersApi.list({ role: 'MANAGER', size: 100 }).then((p) => setManagers(p.content)).catch(() => setManagers([]));
  }, []);

  const submit = async (e: FormEvent) => {
    e.preventDefault();
    setError(null);
    const payload = {
      name,
      email,
      role,
      password: password || undefined,
      managerId: role === 'ADMIN' || managerId === '' ? null : managerId,
    };
    try {
      if (user) {
        await usersApi.update(user.id, payload);
        onSaved(`${name} foi atualizado.`);
      } else {
        await usersApi.create(payload);
        onSaved(`${name} foi criado.`);
      }
    } catch (err) {
      setError((err as Error).message);
    }
  };

  return (
    <Modal title={user ? 'Editar colaborador' : 'Novo colaborador'} onClose={onClose}>
      <form className="form" onSubmit={submit}>
        <Alert message={error} />
        <label>
          Nome
          <input value={name} onChange={(e) => setName(e.target.value)} required maxLength={100} />
        </label>
        <label>
          Email
          <input type="email" value={email} onChange={(e) => setEmail(e.target.value)} required maxLength={150} />
        </label>
        <label>
          Password {user && <span className="muted small">(deixe vazio para manter)</span>}
          <input type="password" value={password} onChange={(e) => setPassword(e.target.value)}
                 required={!user} minLength={6} maxLength={72} />
        </label>
        <div className="grid-2">
          <label>
            Role
            <select value={role} onChange={(e) => setRole(e.target.value as Role)}>
              <option value="COLLABORATOR">Colaborador</option>
              <option value="MANAGER">Manager</option>
              <option value="ADMIN">Admin</option>
            </select>
          </label>
          {role !== 'ADMIN' && (
            <label>
              Manager {role === 'COLLABORATOR' ? '' : <span className="muted small">(opcional)</span>}
              <select value={managerId} onChange={(e) => setManagerId(e.target.value ? Number(e.target.value) : '')}
                      required={role === 'COLLABORATOR'}>
                <option value="">— Selecionar —</option>
                {managers.filter((m) => m.id !== user?.id).map((m) => (
                  <option key={m.id} value={m.id}>{m.name}</option>
                ))}
              </select>
            </label>
          )}
        </div>
        <div className="form-actions">
          <button type="button" className="btn" onClick={onClose}>Voltar</button>
          <button type="submit" className="btn btn-primary">Guardar</button>
        </div>
      </form>
    </Modal>
  );
}
