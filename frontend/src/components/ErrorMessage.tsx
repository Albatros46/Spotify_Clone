import { ApiError } from '../api/client';

/** Shows the backend's message and, for validation errors, every field error. */
export function ErrorMessage({ error }: { error: unknown }) {
  if (!error) {
    return null;
  }
  const message = error instanceof Error ? error.message : 'Something went wrong';
  const details = error instanceof ApiError ? error.errors : [];
  return (
    <div className="error" role="alert">
      <div>{message}</div>
      {details.length > 0 && (
        <ul>
          {details.map((d) => <li key={d}>{d}</li>)}
        </ul>
      )}
    </div>
  );
}
