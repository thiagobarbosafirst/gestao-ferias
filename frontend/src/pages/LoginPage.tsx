import { useState, type FormEvent } from 'react';
import { useAuth } from '../auth';
import { Alert } from '../components/Alert';

/** Contas criadas pelo DataSeeder do backend (password: password123). */
const DEMO_ACCOUNTS = [
  { email: 'admin@taskflow.com', label: 'Ana Admin', role: 'Admin' },
  { email: 'marco@taskflow.com', label: 'Marco Manager', role: 'Manager (João, Maria)' },
  { email: 'sofia@taskflow.com', label: 'Sofia Santos', role: 'Manager (Pedro, Rita)' },
  { email: 'joao@taskflow.com', label: 'João Silva', role: 'Colaborador' },
  { email: 'pedro@taskflow.com', label: 'Pedro Alves', role: 'Colaborador' },
];

export function LoginPage() {
  const { login } = useAuth();
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  const submit = async (e: FormEvent) => {
    e.preventDefault();
    setError(null);
    setSubmitting(true);
    try {
      await login(email, password);
    } catch (err) {
      setError((err as Error).message);
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="login-page">
      <form className="card login-card" onSubmit={submit}>
        <div className="brand big">TaskFlow <span>Férias</span></div>
        <p className="muted">Entre com a sua conta para gerir pedidos de férias.</p>
        <Alert message={error} />
        <label>
          Email
          <input type="email" value={email} onChange={(e) => setEmail(e.target.value)} required autoFocus />
        </label>
        <label>
          Password
          <input type="password" value={password} onChange={(e) => setPassword(e.target.value)} required />
        </label>
        <button className="btn btn-primary" type="submit" disabled={submitting}>
          {submitting ? 'A entrar…' : 'Entrar'}
        </button>

        <div className="demo">
          <div className="muted small">Contas de demonstração (password <code>password123</code>):</div>
          {DEMO_ACCOUNTS.map((a) => (
            <button type="button" key={a.email} className="demo-account"
                    onClick={() => { setEmail(a.email); setPassword('password123'); }}>
              <strong>{a.label}</strong> <span className="muted small">{a.role}</span>
            </button>
          ))}
        </div>
      </form>
    </div>
  );
}
