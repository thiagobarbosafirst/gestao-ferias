import type { Page } from '../types';

/** Controlos "anterior / seguinte" para respostas paginadas do backend (page começa em 0). */
export function Pagination<T>({ page, onChange }: { page: Page<T>; onChange: (page: number) => void }) {
  if (page.totalElements === 0) return null;
  return (
    <div className="pagination">
      <span className="muted">
        {page.totalElements} resultado{page.totalElements === 1 ? '' : 's'} · página {page.page + 1} de {Math.max(page.totalPages, 1)}
      </span>
      <div className="row">
        <button className="btn" disabled={page.page === 0} onClick={() => onChange(page.page - 1)}>‹ Anterior</button>
        <button className="btn" disabled={page.page + 1 >= page.totalPages} onClick={() => onChange(page.page + 1)}>Seguinte ›</button>
      </div>
    </div>
  );
}
