# Smart Campus API

This is a RESTful API built using JAX-RS (Jersey) and Grizzly, designed to manage rooms, sensors, and sensor readings in a Smart Campus environment.

## Features
- **Project Bootstrap:** Standalone JAX-RS application using Grizzly embedded server.
- **Discovery Endpoint:** HATEOAS-compliant discovery at the API root.
- **Room Management:** CRUD operations for university rooms with safety logic.
- **Sensor Operations:** Register and filter sensors by type.
- **Sensor Readings:** Nested sub-resource for historical sensor data with status-aware validation.
- **Error Handling:** Custom exception mappers for consistent JSON error responses (403, 409, 422, 500).
- **Logging:** Request/Response filters for audit trailing.

## Build & Launch Instructions

### Prerequisites
- Java 8 or higher
- Maven

### Steps
1. **Clone the repository:**
   ```bash
   git clone <repository-url>
   cd SmartCampus
   ```
2. **Build the project:**
   ```bash
   mvn clean install
   ```
3. **Run the application:**
   ```bash
   mvn exec:java
   ```
   The server will start at `http://localhost:8080/api/v1`.

## Sample curl Commands

```bash
# 1. Discovery
curl -X GET http://localhost:8080/api/v1

# 2. Create a Room
curl -X POST http://localhost:8080/api/v1/rooms \
  -H "Content-Type: application/json" \
  -d '{"id":"LIB-301","name":"Library Quiet Study","capacity":50}'

# 3. Get all Rooms
curl -X GET http://localhost:8080/api/v1/rooms

# 4. Create a Sensor (Links to LIB-301)
curl -X POST http://localhost:8080/api/v1/sensors \
  -H "Content-Type: application/json" \
  -d '{"id":"TEMP-001","type":"Temperature","status":"ACTIVE","currentValue":22.5,"roomId":"LIB-301"}'

# 5. Filter sensors by type
curl -X GET "http://localhost:8080/api/v1/sensors?type=Temperature"

# 6. Post a reading (Updates TEMP-001)
curl -X POST http://localhost:8080/api/v1/sensors/TEMP-001/readings \
  -H "Content-Type: application/json" \
  -d '{"id":"READ-001","timestamp":1713960000000,"value":23.1}'

# 7. Delete a room (Returns 409 if sensors exist)
curl -X DELETE http://localhost:8080/api/v1/rooms/LIB-301
```

---

## Report Questions

### 1.1 JAX-RS Lifecycle & Thread Safety
**Question:** Explain the default lifecycle of a JAX-RS Resource class. How does this impact managing in-memory data structures?
**Answer:** By default, JAX-RS resource classes are **request-scoped**, meaning a new instance is created for every HTTP request. This means instance variables are not preserved between calls. To manage in-memory data like our room/sensor maps, we must use `static` variables or a **Singleton** lifecycle. However, static shared state introduces concurrency risks; we must use thread-safe collections (like `ConcurrentHashMap`) or explicit synchronization to prevent race conditions when multiple users access the API simultaneously.

### 1.2 HATEOAS Benefits
**Question:** Why is HATEOAS considered a hallmark of advanced RESTful design? How does it benefit client developers?
**Answer:** HATEOAS (Hypermedia as the Engine of Application State) enables a self-discovering API. Instead of hardcoding URLs, clients follow links provided in responses. This benefits developers by reducing maintenance—server-side URL changes don't break clients—and makes the API easier to explore without constantly referencing static documentation.

### 2.1 IDs only vs Full Objects
**Question:** What are the implications of returning only IDs vs full room objects?
**Answer:** Returning only IDs minimizes network bandwidth (payload size), but forces the client to make extra "detail" requests (the N+1 problem), increasing latency. Returning full objects is "chattier" initially but reduces the total number of round-trips, often providing a smoother user experience at the cost of higher per-request data usage.

### 2.2 DELETE Idempotency
**Question:** Is DELETE idempotent in your implementation?
**Answer:** Yes. In this implementation, calling `DELETE` once removes the resource. Calling it a second time results in no change to the server's state (the resource remains gone). Whether the server returns 200 (OK) or 204 (No Content) for a missing resource, the outcome is identical, fulfilling the definition of idempotency.

### 3.1 @Consumes Mismatch
**Question:** What happens technically if a client sends text/plain instead of JSON?
**Answer:** When the `@Consumes` annotation is set to `application/json` and a client sends a different `Content-Type`, JAX-RS automatically intercepts the request and returns an **HTTP 415 Unsupported Media Type** response before the method logic is ever executed.

### 3.2 Filtering: QueryParam vs PathParam
**Question:** Why is the query parameter approach superior for filtering collections?
**Answer:** Path parameters identify a specific "entity" (a noun), while query parameters are used to "modify" the view of a collection (adjectives/filters). Query parameters are superior because they are optional and composable; you can easily add multiple filters (e.g., `?type=CO2&status=ACTIVE`) without bloating the URL path structure.

### 4.1 Sub-Resource Locator Pattern
**Question:** Discuss the architectural benefits of the Sub-Resource Locator pattern.
**Answer:** This pattern allows us to delegate sub-operations (like sensor readings) to separate, specialized classes. This keeps our main `SensorResource` clean and prevents it from becoming a "God Class." It improves readability, allows for better code reuse, and maps logically to the hierarchical nature of the data.

### 5.2 422 vs 404 Accuracy
**Question:** Why is HTTP 422 more semantically accurate than 404 for missing references in payloads?
**Answer:** A `404 Not Found` implies the requested URL does not exist. However, if the URL is correct but the metadata inside the JSON (like a `roomId`) points to something non-existent, the request is "Unprocessable." `422 Unprocessable Entity` tells the client: "I understood your request structure, but the business logic cannot handle these specific values."

### 5.4 Cybersecurity Risks of Stack Traces
**Question:** What risks come from exposing internal Java stack traces?
**Answer:** Stack traces reveal sensitive "fingerprinting" info: internal package names, third-party library versions, and logic flow. An attacker can use this to identify specific known CVEs in those libraries or understand the internal architecture to craft more sophisticated injection or bypass attacks.

### 5.5 JAX-RS Filters Benefits
**Question:** Why use JAX-RS filters for cross-cutting concerns like logging?
**Answer:** Filters implement the "Don't Repeat Yourself" (DRY) principle. Instead of manually adding logger calls to 10+ methods, a filter captures every request/response automatically. This ensures consistency, simplifies the core business logic, and makes the system easier to audit and maintain.
