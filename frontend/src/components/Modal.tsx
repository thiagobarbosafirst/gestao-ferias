import type { ReactNode } from 'react';

/** Janela modal simples. Fecha ao clicar fora ou no "×". */
export function Modal({ title, onClose, children }: { title: string; onClose: () => void; children: ReactNode }) {
  return (
    <div className="modal-backdrop" onMouseDown={onClose}>
      <div className="modal" role="dialog" aria-label={title} onMouseDown={(e) => e.stopPropagation()}>
        <div className="modal-header">
          <h2>{title}</h2>
          <button className="icon-btn" onClick={onClose} aria-label="Fechar">×</button>
        </div>
        {children}
      </div>
    </div>
  );
}
