import { useState, type FormEvent } from 'react';
import { useNavigate } from 'react-router';

import { songApi } from '../api/endpoints';
import { ErrorMessage } from '../components/ErrorMessage';

export function UploadPage() {
  const navigate = useNavigate();
  const [error, setError] = useState<unknown>(null);
  const [busy, setBusy] = useState(false);

  async function submit(e: FormEvent<HTMLFormElement>) {
    e.preventDefault();
    const form = new FormData(e.currentTarget);
    const image = form.get('image');
    if (image instanceof File && image.size === 0) {
      form.delete('image'); // optional cover: do not send an empty part
    }
    setBusy(true);
    setError(null);
    try {
      await songApi.upload(form);
      navigate('/');
    } catch (err) {
      setError(err);
    } finally {
      setBusy(false);
    }
  }

  return (
    <section>
      <h1>Upload a song</h1>
      <form className="card form" onSubmit={submit}>
        <label>Title <input name="title" required maxLength={100} /></label>
        <label>Artist <input name="artist" required maxLength={100} /></label>
        <label>Album <input name="album" required maxLength={100} /></label>
        <label>Audio file (mp3, wav, ogg, m4a, flac)
          <input name="file" type="file" accept="audio/*" required />
        </label>
        <label>Cover image (optional: jpg, png, webp)
          <input name="image" type="file" accept="image/*" />
        </label>
        <ErrorMessage error={error} />
        <button type="submit" className="primary" disabled={busy}>{busy ? 'Uploading…' : 'Upload'}</button>
      </form>
    </section>
  );
}
