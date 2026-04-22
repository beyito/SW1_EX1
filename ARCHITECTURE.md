# BPMN Engine Architecture

## Final Monorepo Tree

```text
/
|-- backend/               # Spring Boot API + GraphQL + WebSocket + Redis + S3
|   |-- src/
|   |-- pom.xml
|   `-- Dockerfile
|-- frontend/              # Angular standalone app + JointJS designer
|   |-- src/
|   |-- package.json
|   `-- Dockerfile
|-- infra/
|   |-- docker-compose.yml
|   `-- nginx.conf
|-- .env.example
`-- ARCHITECTURE.md
```

## Attachment Upload Flow to Amazon S3

1. User selects a file in the policy designer task form.
2. Angular calls `FileService.uploadAttachment(file, policyId)` immediately.
3. Backend endpoint `POST /api/files/upload` receives multipart data.
4. `S3Service` uploads file bytes to `s3://<bucket>/policies/{policyId}/attachments/...`.
5. Backend generates a presigned GET URL and returns `{ key, url, contentType, size }`.
6. Frontend stores the returned `url` (not local file path) in the diagram JSON attachment.
7. Auto-save persists the updated diagram JSON; no manual disk uploads and no `C:\fakepath` leakage.

## Real-time Collaboration Across Containers

- Client sends STOMP events to `/app/policy/{policyId}/change`.
- Backend controller publishes event envelopes into Redis channel `designer-events`.
- Every backend instance listens to Redis pub/sub.
- Listener forwards events to WebSocket destination `/topic/policy/{policyId}`.
- Connected clients in all containers receive updates consistently.

## Container Communication (Docker)

- `frontend` (Nginx): exposes `:4200` -> serves Angular.
- `backend` (Spring): exposes `:8080` -> REST/GraphQL/WebSocket.
- `redis`: exposes `:6379` -> collaborative event bus.
- `mongo`: exposes `:27017` -> current persistence.
- `postgres`: exposes `:5432` -> RDS simulation placeholder.

Nginx reverse proxy routes:

- `/api/*` -> `backend:8080/api/*`
- `/graphql` -> `backend:8080/graphql`
- `/ws-designer` -> `backend:8080/ws-designer` (WebSocket Upgrade enabled)

All services share network `bpmn-net`.

## Notes for ECS/Fargate

- Do not hardcode secrets; inject from Secrets Manager/SSM.
- Keep S3 bucket private and use presigned URLs.
- Run Redis as managed ElastiCache in production.
- Replace local Mongo/Postgres containers with managed services (DocumentDB/RDS).
