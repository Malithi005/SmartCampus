# 5COSC022W — Smart Campus API: Implementation Plan

**Due:** 24th April 2026, 13:00
**Weight:** 60% of final grade | **Qualifying Mark:** 30%
**Stack:** JAX-RS only (Jersey) + Maven | No Spring Boot | No Database

---
## Mark Split Per Task (Remember!)

| Component | Weight |
|-----------|--------|
| Coding | 50% |
| Video Demo | 30% |
| Report / Questions | 20% |

---

## ⚠️ Critical Rules — Instant ZERO if Violated

- [ ] Using Spring Boot or any non-JAX-RS framework
- [ ] Submitting a ZIP file instead of GitHub repo
- [ ] Using any database (SQL, etc.) — only HashMap / ArrayList allowed

---

## Phase 1: Project Bootstrap

**Goal:** Get the project running with a base URL before touching any features.

### Tasks

- [ ] Create Maven project structure
- [ ] Add Jersey + embedded server (e.g., Grizzly) to `pom.xml`
- [ ] Create `SmartCampusApplication` extending `javax.ws.rs.core.Application`
- [ ] Annotate with `@ApplicationPath("/api/v1")`
- [ ] Create shared in-memory stores:
  ```java
  static Map<String, Room> rooms = new HashMap<>();
  static Map<String, Sensor> sensors = new HashMap<>();
  static Map<String, List<SensorReading>> readings = new HashMap<>();
  ```
- [ ] Verify server starts and `http://localhost:8080/api/v1` is reachable

### POJOs to Create

- [ ] `Room.java` — id, name, capacity, List\<String\> sensorIds
- [ ] `Sensor.java` — id, type, status, currentValue, roomId
- [ ] `SensorReading.java` — id, timestamp, value

### Report Question 1.1
> Explain the default lifecycle of a JAX-RS Resource class (request-scoped vs singleton). How does this impact managing in-memory data structures to prevent race conditions?

---

## Phase 2: Discovery Endpoint

**Endpoint:** `GET /api/v1`
**Marks:** 5

### Tasks

- [ ] Create `DiscoveryResource.java`
- [ ] Return JSON with:
  - API version
  - Admin contact details
  - Resource map: `"rooms" -> "/api/v1/rooms"`, `"sensors" -> "/api/v1/sensors"`
- [ ] Test in Postman — confirm valid JSON response

### Report Question 1.2
> Why is HATEOAS considered a hallmark of advanced RESTful design? How does it benefit client developers compared to static documentation?

---

## Phase 3: Room Management

**Endpoint:** `/api/v1/rooms`
**Marks:** 20

### 3.1 Room CRUD (10 Marks)

- [ ] Create `RoomResource.java` mapped to `/api/v1/rooms`
- [ ] `GET /` — return list of all rooms
- [ ] `POST /` — create room, return **201 Created** + `Location` header
- [ ] `GET /{roomId}` — return single room details (404 if not found)

**Video Demo:**
- POST a room → show 201 + Location header
- GET by the new room ID

### Report Question 2.1
> What are the implications of returning only IDs vs full room objects? Consider network bandwidth and client-side processing.

---

### 3.2 Room Deletion & Safety Logic (10 Marks)

- [ ] `DELETE /{roomId}` — delete room if it exists
- [ ] **Business Rule:** If room has sensors in `sensorIds` list → throw `RoomNotEmptyException`
- [ ] Return 200/204 on success
- [ ] Return 409 Conflict (via mapper) if sensors exist

**Video Demo:**
- DELETE a room → show success
- DELETE a room that has sensors → show 409 Conflict

### Report Question 2.2
> Is DELETE idempotent in your implementation? Justify by describing what happens if the same DELETE request is sent multiple times.

---

## Phase 4: Sensor Operations

**Endpoint:** `/api/v1/sensors`
**Marks:** 20

### 4.1 Sensor Registration & Integrity (10 Marks)

- [ ] Create `SensorResource.java` mapped to `/api/v1/sensors`
- [ ] `POST /` — register new sensor:
  - Validate `roomId` exists in rooms map
  - If not → throw `LinkedResourceNotFoundException` (422)
  - If valid → add sensor, add sensorId to room's sensorIds list
- [ ] Use `@Consumes(MediaType.APPLICATION_JSON)` on POST

**Video Demo:**
- POST sensor with non-existent roomId → show error
- POST sensor with valid roomId → show success

### Report Question 3.1
> What happens technically if a client sends `text/plain` or `application/xml` instead of JSON? How does JAX-RS handle the `@Consumes` mismatch?

---

### 4.2 Filtered Retrieval (10 Marks)

- [ ] `GET /` — return all sensors
- [ ] Support optional `?type=` query param: `GET /api/v1/sensors?type=CO2`
- [ ] If type provided → filter list by type (case-insensitive recommended)
- [ ] If no param → return all

**Video Demo:**
- GET `/sensors?type=CO2` → change parameter → show dynamic list update

### Report Question 3.2
> Contrast `@QueryParam` filtering vs path-based design (e.g., `/sensors/type/CO2`). Why is the query parameter approach superior for filtering collections?

---

## Phase 5: Sub-Resources (Sensor Readings)

**Endpoint:** `/api/v1/sensors/{sensorId}/readings`
**Marks:** 20

### 5.1 Sub-Resource Locator (10 Marks)

- [ ] In `SensorResource`, add a sub-resource locator method:
  ```java
  @Path("{sensorId}/readings")
  public SensorReadingResource getReadingResource(@PathParam("sensorId") String sensorId) {
      return new SensorReadingResource(sensorId);
  }
  ```
- [ ] Create separate `SensorReadingResource.java` class
- [ ] Confirm the path `/sensors/{id}/readings` resolves correctly

**Video Demo:**
- Navigate to `/sensors/{id}/readings` in Postman
- Show nested structure

### Report Question 4.1
> Discuss the architectural benefits of the Sub-Resource Locator pattern. How does delegating to separate classes help manage complexity vs one massive controller?

---

### 5.2 Historical Data Management (10 Marks)

- [ ] In `SensorReadingResource`:
  - `GET /` — return all readings for that sensor
  - `POST /` — append new reading to sensor's reading list
- [ ] **Side Effect on POST:** Update parent `Sensor.currentValue` with the new reading's value
- [ ] Check sensor status — if `"MAINTENANCE"` → throw `SensorUnavailableException` (403)

**Video Demo:**
- POST a reading → show it appears in GET history
- Show parent sensor's `currentValue` updated

---

## Phase 6: Error Handling & Logging

**Marks:** 30
**Goal:** API must NEVER expose raw stack traces or default server error pages.

### 6.1 RoomNotEmptyException → 409 Conflict (5 Marks)

- [ ] Create `RoomNotEmptyException.java`
- [ ] Create `RoomNotEmptyExceptionMapper.java` implementing `ExceptionMapper<RoomNotEmptyException>`
- [ ] Return HTTP 409 with JSON body:
  ```json
  { "error": "Conflict", "message": "Room still has active sensors assigned." }
  ```

---

### 6.2 LinkedResourceNotFoundException → 422 (10 Marks)

- [ ] Create `LinkedResourceNotFoundException.java`
- [ ] Create `LinkedResourceNotFoundExceptionMapper.java`
- [ ] Return HTTP 422 Unprocessable Entity with JSON body:
  ```json
  { "error": "Unprocessable Entity", "message": "Referenced roomId does not exist." }
  ```

**Video Demo:** Show 422 with JSON body

### Report Question 5.2
> Why is HTTP 422 more semantically accurate than 404 when the issue is a missing reference inside a valid JSON payload?

---

### 6.3 SensorUnavailableException → 403 Forbidden (5 Marks)

- [ ] Create `SensorUnavailableException.java`
- [ ] Create `SensorUnavailableExceptionMapper.java`
- [ ] Return HTTP 403 with JSON body:
  ```json
  { "error": "Forbidden", "message": "Sensor is under maintenance and cannot accept readings." }
  ```

**Video Demo:** Show 403 with JSON body

---

### 6.4 Global Safety Net → 500 (5 Marks)

- [ ] Create `GlobalExceptionMapper.java` implementing `ExceptionMapper<Throwable>`
- [ ] Return HTTP 500 with clean JSON body — NO stack trace:
  ```json
  { "error": "Internal Server Error", "message": "An unexpected error occurred." }
  ```

**Video Demo:** Trigger a 500 → prove NO stack trace in response

### Report Question 5.4
> From a cybersecurity standpoint, what risks come from exposing internal Java stack traces? What specific info could an attacker gather?

---

### 6.5 Request & Response Logging Filter (5 Marks)

- [ ] Create `LoggingFilter.java` implementing **both**:
  - `ContainerRequestFilter`
  - `ContainerResponseFilter`
- [ ] In request filter: log HTTP method + URI
- [ ] In response filter: log HTTP status code
- [ ] Use `java.util.logging.Logger`

### Report Question 5.5
> Why is it better to use JAX-RS filters for cross-cutting concerns like logging, rather than adding `Logger.info()` in every resource method?

---

## Phase 7: GitHub & Submission Prep

### README.md Must Include

- [ ] Overview of API design
- [ ] Step-by-step build & launch instructions
- [ ] At least **5 sample curl commands** covering different parts of the API
- [ ] Answers to ALL report questions (this IS your report)

### Sample curl Commands to Include

```bash
# 1. Discovery
curl -X GET http://localhost:8080/api/v1

# 2. Create a Room
curl -X POST http://localhost:8080/api/v1/rooms \
  -H "Content-Type: application/json" \
  -d '{"id":"LIB-301","name":"Library Quiet Study","capacity":50}'

# 3. Get all Rooms
curl -X GET http://localhost:8080/api/v1/rooms

# 4. Create a Sensor
curl -X POST http://localhost:8080/api/v1/sensors \
  -H "Content-Type: application/json" \
  -d '{"id":"TEMP-001","type":"Temperature","status":"ACTIVE","currentValue":22.5,"roomId":"LIB-301"}'

# 5. Filter sensors by type
curl -X GET "http://localhost:8080/api/v1/sensors?type=Temperature"

# 6. Post a reading
curl -X POST http://localhost:8080/api/v1/sensors/TEMP-001/readings \
  -H "Content-Type: application/json" \
  -d '{"id":"READ-001","timestamp":1713960000000,"value":23.1}'

# 7. Delete a room (with sensors = 409)
curl -X DELETE http://localhost:8080/api/v1/rooms/LIB-301
```

### Final Submission Checklist

- [ ] Public GitHub repo created and working
- [ ] README.md complete with all questions answered
- [ ] Video (max 10 min) recorded — camera + mic working
- [ ] You appear and speak clearly in the video
- [ ] Video uploaded to Blackboard
- [ ] PDF report prepared (questions only, no intro/test cases)
- [ ] Blackboard submission link submitted before **24th April 2026, 13:00**

---

## Report Questions Summary

| Section | Question Topic |
|---------|---------------|
| 1.1 | JAX-RS lifecycle: request-scoped vs singleton + thread safety |
| 1.2 | HATEOAS benefits vs static documentation |
| 2.1 | IDs only vs full objects — bandwidth & processing trade-offs |
| 2.2 | Is DELETE idempotent? Justify with multiple call scenarios |
| 3.1 | @Consumes mismatch — what happens with wrong content-type? |
| 3.2 | @QueryParam vs @PathParam for filtering — why query is better |
| 4.1 | Sub-Resource Locator — architectural benefits & complexity |
| 5.2 | Why 422 is more accurate than 404 for missing reference in payload |
| 5.4 | Cybersecurity risks of exposing stack traces |
| 5.5 | JAX-RS filters vs manual logging in every method |

---

*Good luck! Focus on getting clean JSON responses and proper status codes — that's what the rubric rewards most.*