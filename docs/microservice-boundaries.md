# Microservice Boundary Notes

## Target Stack

- Backend: Spring Boot + Spring MVC + MyBatis + Redis.
- Frontend: Vue 3 + Element Plus.
- Redis belongs to backend infrastructure. It is used for tokens, login lockout, cache, idempotency, and high-concurrency course selection.

The current codebase still contains JPA-based persistence from the first prototype. New persistence work should move toward MyBatis mapper style.

## First Candidate Service: Course Grab Service

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
