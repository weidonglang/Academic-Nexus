# Microservice Boundary Notes

## Current Spring Cloud Stack

- Backend: Spring Boot + Spring MVC + MyBatis + Redis.
- Service discovery: Spring Cloud Alibaba Nacos Discovery.
- Service call: Spring Cloud OpenFeign + LoadBalancer.
- Frontend: Vue 3 + Element Plus.
- Redis belongs to backend infrastructure. It is used for tokens, login lockout, cache, idempotency, and high-concurrency course selection.

The current codebase still contains JPA-based persistence from the first prototype. New persistence work should move toward MyBatis mapper style.

## Implemented Service Boundary: AI Service

The first real Spring Cloud boundary is the AI service:

```text
academic-main:8080
  -> OpenFeign + LoadBalancer
  -> academic-ai-service:8090
```

Service names:

```text
spring.application.name=academic-main
spring.application.name=academic-ai-service
```

Nacos defaults:

```text
NACOS_ADDR=127.0.0.1:8848
NACOS_DISCOVERY_ENABLED=false
NACOS_REGISTER_ENABLED=false
AI_SERVICE_DISCOVERY_ENABLED=false
```

For local demos without Nacos, the main system keeps using:

```text
AI_SERVICE_URL=http://localhost:8090
```

For Spring Cloud demos, enable the three discovery flags. Then the main system calls the AI service by service name instead of a fixed URL. Multiple `academic-ai-service` instances can be started on different ports and discovered through Nacos.

Current Feign contract:

```text
POST /internal/ai/rag/answer
POST /internal/ai/chat
POST /internal/ai/load-test/analyze
POST /internal/ai/sql/generate
GET  /internal/ai/status
```

## Next Candidate Service: Course Grab Service

Course selection is the best first microservice boundary because it has:

- high write concurrency during selection windows;
- strict capacity and duplicate-selection rules;
- clear ownership of Redis capacity keys and locks;
- a narrow API surface;
- independent scaling needs.

## Current Port

The application now depends on:

```text
CourseGrabPort
```

Current implementation:

```text
LocalCourseGrabService
```

Future implementation:

```text
RemoteCourseGrabClient
```

The placeholder `RemoteCourseGrabClient` is now wired behind:

```text
course-grab.remote.enabled=true
```

When disabled, the app uses `LocalCourseGrabService`.

The controller and frontend can keep using:

```text
POST /api/course-selection/grab/offerings/{offeringId}
```

## Future Remote Contract

Suggested request:

```json
{
  "username": "student-number-or-work-id",
  "offeringId": 1,
  "requestId": "uuid-for-idempotency"
}
```

Suggested response:

```json
{
  "selectionId": 1,
  "offeringId": 1,
  "courseCode": "CS301",
  "status": "SUCCESS",
  "message": "Course selected"
}
```

## Redis Keys

Suggested keys:

```text
selection:grab:lock:{offeringId}:{username}
selection:offering:{offeringId}:remaining
selection:request:{requestId}
```

The database remains the source of truth. Redis is used for concurrency control and short-lived state.
