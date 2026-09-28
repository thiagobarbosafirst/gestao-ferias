/** Mensagem de erro/sucesso. Não mostra nada se a mensagem for vazia. */
export function Alert({ message, kind = 'error' }: { message: string | null; kind?: 'error' | 'success' }) {
  if (!message) return null;
  return <div className={`alert alert-${kind}`} role="alert">{message}</div>;
}
