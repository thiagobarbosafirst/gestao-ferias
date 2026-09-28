import { useState } from 'react';
import { useAuth } from './auth';
import { ROLE_LABELS } from './types';
import { LoginPage } from './pages/LoginPage';
import { VacationsPage } from './pages/VacationsPage';
import { CalendarPage } from './pages/CalendarPage';
import { UsersPage } from './pages/UsersPage';

type Tab = 'vacations' | 'calendar' | 'users';

/**
 * Estrutura principal: se não houver sessão mostra o login; caso contrário, o cabeçalho com
 * separadores. O separador "Colaboradores" só aparece para ADMIN (o backend também o impõe).
 */
export default function App() {
  const { user, loading, logout } = useAuth();
  const [tab, setTab] = useState<Tab>('vacations');

  if (loading) return <div className="center muted">A carregar…</div>;
  if (!user) return <LoginPage />;

  const tabs: { id: Tab; label: string }[] = [
    { id: 'vacations', label: 'Pedidos de férias' },
    { id: 'calendar', label: 'Calendário' },
    ...(user.role === 'ADMIN' ? [{ id: 'users' as Tab, label: 'Colaboradores' }] : []),
  ];

  // Se o separador escolhido não existir para este utilizador (ex.: login com outro role), volta ao primeiro.
  const current: Tab = tabs.some((t) => t.id === tab) ? tab : 'vacations';

  return (
    <div className="app">
      <header className="topbar">
        <div className="brand">TaskFlow <span>Férias</span></div>
        <nav className="tabs">
          {tabs.map((t) => (
            <button key={t.id} className={`tab ${current === t.id ? 'active' : ''}`} onClick={() => setTab(t.id)}>
              {t.label}
            </button>
          ))}
        </nav>
        <div className="user-box">
          <div>
            <div className="user-name">{user.name}</div>
            <div className="muted small">{ROLE_LABELS[user.role]}</div>
          </div>
          <button className="btn" onClick={logout}>Sair</button>
        </div>
      </header>
      <main className="content">
        {current === 'vacations' && <VacationsPage />}
        {current === 'calendar' && <CalendarPage />}
        {current === 'users' && <UsersPage />}
      </main>
    </div>
  );
}
