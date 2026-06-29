# EduNexus AI Docker Troubleshooting

This guide focuses on reproducible EduNexus AI Docker builds. Historical service names such as `academic-main` and `academic-ai-service` are kept for compatibility.

## Maven `bad_record_mac` Or Central Download Failures

`bad_record_mac`, `Could not transfer artifact`, and intermittent Maven Central TLS errors happen while Docker downloads dependencies. They are network, TLS, proxy, or mirror problems, not business-code failures.

Retry with BuildKit cache:

```powershell
.\scripts\docker-build.ps1
```

Switch mirror when Central is unstable:

```powershell
.\scripts\docker-build.ps1 -MavenMirror aliyun
```

Linux or macOS:

```bash
MAVEN_MIRROR=aliyun ./scripts/docker-build.sh
```

Clean Docker build cache only when needed:

```powershell
docker builder prune
docker compose build --no-cache academic-main
```

Build one service:

```powershell
.\scripts\docker-build.ps1 -Service academic-main
.\scripts\docker-build.ps1 -Service academic-ai-service
```

Show more Maven detail by running a one-off build command:

```powershell
$env:MAVEN_MIRROR_URL="https://maven.aliyun.com/repository/public"
docker compose build --progress=plain academic-main
```

If your network requires a proxy, configure Docker Desktop proxy settings or export `HTTP_PROXY`, `HTTPS_PROXY`, and `NO_PROXY` before building.

## Host Port Conflicts

EduNexus AI keeps container-internal ports stable and maps host ports through `.env`.

Default host ports:

| Service | Host | Container |
| --- | --- | --- |
| Frontend | `5174` | `5173` |
| Main backend | `8088` | `8080` |
| MySQL | `13306` | `3306` |
| Redis | `16379` | `6379` |
| Nacos HTTP | `18848` | `8848` |
| Nacos gRPC | `19848` | `9848` |
| AI service | `18090` | `8090` |

Check ports:

```powershell
copy .env.example .env
.\scripts\check-ports.ps1
```

Linux or macOS:

```bash
cp .env.example .env
./scripts/check-ports.sh
```

When a port is occupied, change the matching variable in `.env`, for example:

```env
MYSQL_HOST_PORT=23306
REDIS_HOST_PORT=26379
FRONTEND_HOST_PORT=15173
```

Container-to-container traffic still uses internal names and ports such as `mysql:3306`, `redis:6379`, `nacos:8848`, `academic-ai-service:8090`, and `academic-main:8080`.
