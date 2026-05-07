# mindnote-server

Ktor backend for MindNote. Postgres via Exposed. Single-user demo (`userId=local`).

## Endpoints

- `GET /health` → `ok`
- `GET /notes` → `NoteDto[]`
- `GET /notes/{id}` → `NoteDto` | 404
- `GET /notes/{id}/related` → `NoteDto[]`
- `POST /notes` with `NoteCreateDto` → `NoteDto` (201)
- `GET /favorites` → `NoteDto[]`
- `POST /favorites/{noteId}` → 204
- `DELETE /favorites/{noteId}` → 204

## Env vars

- `PORT` (default `8080`) — Railway sets this automatically.
- `DATABASE_URL` — full Postgres URL (`postgres://user:pass@host:port/db` or `jdbc:postgresql://...`). Railway sets this from the attached Postgres plugin.
- `PGUSER`, `PGPASSWORD` — optional fallback when `DATABASE_URL` is a JDBC URL without credentials.

On first boot the server auto-creates the schema and seeds the 7 demo notes if the `notes` table is empty.

## Local dev (Docker)

```bash
cd server
docker build -t mindnote-server .
docker run --rm -p 8080:8080 \
  -e DATABASE_URL=postgres://postgres:postgres@host.docker.internal:5432/mindnote \
  mindnote-server
```

## Deploy to Railway

From `server/`:

```bash
railway login
railway init                 # create project, or link existing: railway link
railway add --plugin postgres
railway up
```

After `railway up` finishes, get the public URL:

```bash
railway domain
```

Copy that URL and put it in `../local.properties` as:

```
MINDNOTE_API_URL=https://<your-service>.up.railway.app/
```

Then rebuild the Android app.
