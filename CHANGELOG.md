# Changelog

## v1.3.0 - 2026-06-27

- Fixed login fallback behavior by adding safe default access and refresh token TTL values.
- Fixed multi-role display priority so admin/teacher/student users render and authorize consistently.
- Fixed AI service offline feedback by returning a readable Spring Cloud fallback message.
- Hardened AI safety configuration and content moderation reads against migration drift to avoid page-level 500 errors.
- Improved frontend error feedback on AI model administration, teaching evaluation details, notice management, registration applications, and status-change pages.
- Added real HTTP regression coverage for #39, #41, and #44.
- Added v1.3.0 QA closure documentation for issues #39-#59, including explicit out-of-scope module boundaries.

## v1.2 - 2026-06-27

- Added Spring Cloud OpenFeign, LoadBalancer, Nacos discovery wiring, AI model registry, search safety configuration, and release packaging.
- Closed the #4-#35 v1.2 issue matrix with backend tests, frontend build checks, Docker Compose verification, and release docs.

