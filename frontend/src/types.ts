// Tipos que espelham os DTOs do backend (pacote com.taskflow.vacations.dto).

export type Role = 'ADMIN' | 'MANAGER' | 'COLLABORATOR';
export type VacationStatus = 'PENDENTE' | 'APROVADO' | 'REJEITADO';

export interface UserRef {
  id: number;
  name: string;
}

export interface User {
  id: number;
  name: string;
  email: string;
  role: Role;
  manager: UserRef | null;
  createdAt: string;
}

export interface VacationRequest {
  id: number;
  employee: UserRef;
  startDate: string; // yyyy-MM-dd
  endDate: string;
  days: number;
  status: VacationStatus;
  notes: string | null;
  rejectionReason: string | null;
  decidedBy: UserRef | null;
  decidedAt: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface CalendarEntry {
  id: number | null;
  startDate: string;
  endDate: string;
  status: VacationStatus;
  employeeName: string | null;
  visible: boolean;
}

export interface Page<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface FieldError {
  field: string;
  message: string;
}

export const ROLE_LABELS: Record<Role, string> = {
  ADMIN: 'Admin',
  MANAGER: 'Manager',
  COLLABORATOR: 'Colaborador',
};
