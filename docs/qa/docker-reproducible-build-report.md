# Docker Reproducible Build Report

Updated: 2026-06-28

| Area | Result |
| --- | --- |
| Main Dockerfile | BuildKit syntax, Maven cache mount, `dependency:go-offline`, mirror settings |
| AI Dockerfile | Same Maven cache/offline strategy from root build context |
| Compose build args | `MAVEN_MIRROR_URL` with Central default |
| Script | `scripts/docker-build.ps1` and `.sh` |
| Verification | `.\scripts\docker-build.ps1` passed for main, AI service, and frontend |

The build path no longer performs an uncached bare `mvn package` as its first dependency-download step.
