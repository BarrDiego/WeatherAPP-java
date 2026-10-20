import "./StatusMessage.css";

export type StatusMessageVariant = "loading" | "error" | "notice" | "empty";

interface StatusMessageProps {
  variant: StatusMessageVariant;
  message: string;
  onRetry?: () => void;
  retryLabel?: string;
}

export function StatusMessage({
  variant,
  message,
  onRetry,
  retryLabel = "Intentar nuevamente",
}: StatusMessageProps) {
  const isError = variant === "error";

  return (
    <div
      className={`status-message status-message--${variant}`}
      role={isError ? "alert" : "status"}
      aria-live={isError ? "assertive" : "polite"}
      aria-busy={variant === "loading"}
    >
      {variant === "loading" && <span className="status-message__spinner" aria-hidden="true" />}
      <p className="status-message__text">{message}</p>
      {isError && onRetry && (
        <button className="status-message__retry" type="button" onClick={onRetry}>
          {retryLabel}
        </button>
      )}
    </div>
  );
}
