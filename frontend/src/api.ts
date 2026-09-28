import type { CalendarEntry, FieldError, Page, Role, User, VacationRequest, VacationStatus } from './types';

/**
 * Cliente HTTP da API. Todas as chamadas passam por request(), que:
 *  - adiciona o token JWT no header Authorization;
 *  - converte respostas de erro (formato ErrorResponse do backend) numa ApiError com a mensagem certa.
 */

const TOKEN_KEY = 'ferias.token';

export const tokenStorage = {
  get: () => localStorage.getItem(TOKEN_KEY),
  set: (token: string) => localStorage.setItem(TOKEN_KEY, token),
  clear: () => localStorage.removeItem(TOKEN_KEY),
};

export class ApiError extends Error {
  readonly status: number;
  readonly fieldErrors: FieldError[];

  constructor(status: number, message: string, fieldErrors: FieldError[] = []) {
    super(message);
    this.status = status;
    this.fieldErrors = fieldErrors;
  }
}

/** Chamado quando o backend responde 401 (token expirado/inválido). Definido pelo AuthProvider. */
let onUnauthorized: () => void = () => {};
export function setUnauthorizedHandler(handler: () => void) {
  onUnauthorized = handler;
}

async function request<T>(method: string, path: string, body?: unknown): Promise<T> {
  const headers: Record<string, string> = {};
  const token = tokenStorage.get();
  if (token) headers.Authorization = `Bearer ${token}`;
  if (body !== undefined) headers['Content-Type'] = 'application/json';

  const res = await fetch(`/api${path}`, {
    method,
    headers,
    body: body !== undefined ? JSON.stringify(body) : undefined,
  });

  if (res.status === 204) return undefined as T;

  const data = await res.json().catch(() => null);
  if (!res.ok) {
    if (res.status === 401 && path !== '/auth/login') onUnauthorized();
    const fieldErrors: FieldError[] = data?.fieldErrors ?? [];
    const details = fieldErrors.map((f) => f.message).join('; ');
    const message = data?.message ?? `Erro ${res.status}`;
    throw new ApiError(res.status, details ? `${message}: ${details}` : message, fieldErrors);
  }
  return data as T;
}

/** Constrói a query string ignorando valores vazios. */
function query(params: Record<string, string | number | undefined | null>): string {
  const qs = new URLSearchParams();
  Object.entries(params).forEach(([k, v]) => {
    if (v !== undefined && v !== null && v !== '') qs.set(k, String(v));
  });
  const s = qs.toString();
  return s ? `?${s}` : '';
}

// ---------------------------------------------------------------- Auth
export const authApi = {
  login: (email: string, password: string) =>
    request<{ token: string; user: User }>('POST', '/auth/login', { email, password }),
  me: () => request<User>('GET', '/auth/me'),
};

// ---------------------------------------------------------------- Utilizadores (ADMIN)
export interface UserFilters {
  search?: string;
  role?: Role | '';
  managerId?: number;
  page?: number;
  size?: number;
}

export interface UserPayload {
  name: string;
  email: string;
  password?: string;
  role: Role;
  managerId: number | null;
}

export const usersApi = {
  list: (f: UserFilters = {}) => request<Page<User>>('GET', `/users${query({ ...f })}`),
  get: (id: number) => request<User>('GET', `/users/${id}`),
  create: (p: UserPayload) => request<User>('POST', '/users', p),
  update: (id: number, p: UserPayload) => request<User>('PUT', `/users/${id}`, p),
  remove: (id: number) => request<void>('DELETE', `/users/${id}`),
};

// ---------------------------------------------------------------- Pedidos de férias
export interface VacationFilters {
  status?: VacationStatus | '';
  employeeName?: string;
  from?: string;
  to?: string;
  page?: number;
  size?: number;
}

export interface VacationPayload {
  startDate: string;
  endDate: string;
  notes?: string;
  userId?: number | null;
}

export const vacationsApi = {
  list: (f: VacationFilters = {}) => request<Page<VacationRequest>>('GET', `/vacations${query({ ...f })}`),
  calendar: (from: string, to: string) => request<CalendarEntry[]>('GET', `/vacations/calendar${query({ from, to })}`),
  create: (p: VacationPayload) => request<VacationRequest>('POST', '/vacations', p),
  update: (id: number, p: VacationPayload) => request<VacationRequest>('PUT', `/vacations/${id}`, p),
  cancel: (id: number) => request<void>('DELETE', `/vacations/${id}`),
  approve: (id: number) => request<VacationRequest>('PATCH', `/vacations/${id}/approve`),
  reject: (id: number, reason: string) => request<VacationRequest>('PATCH', `/vacations/${id}/reject`, { reason }),
};
