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
   git clone https://github.com/Malithi005/SmartCampus.git
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

**Student Name:** Malithi Nadunika  
**Student ID:** W2153611  

### Part 1: Service Architecture & Setup

#### 1.1 JAX-RS Lifecycle & Thread Safety
**Question:** Explain the default lifecycle of a JAX-RS Resource class. How does this impact managing in-memory data structures?
**Answer:** By default, JAX-RS resource classes are **request-scoped**. A new instance is created for every HTTP request and discarded after the response is sent. This means instance variables are not persisted across requests. To maintain data like our room and sensor maps, we must use `static` variables or a `Singleton` lifecycle. Since these static collections (like `ConcurrentHashMap`) are shared across threads handling different requests, we must use thread-safe data structures or explicit synchronization to prevent race conditions and data corruption.

#### 1.2 HATEOAS Benefits
**Question:** Why is the provision of ”Hypermedia” (links and navigation within responses) considered a hallmark of advanced RESTful design (HATEOAS)? How does it benefit client developers?
**Answer:** HATEOAS makes the API self-documenting and discoverable. Instead of hardcoding URLs, clients follow links provided in responses. This benefits developers by reducing coupling between the client and server; the server can change its URI structure without breaking clients, as long as the relationship names (rel) remain consistent. It also makes the API easier to explore without constantly referencing static documentation, as the state transitions are guided by the server.

### Part 2: Room Management

#### 2.1 IDs only vs Full Objects
**Question:** What are the implications of returning only IDs versus returning the full room objects? Consider network bandwidth and client side processing.
**Answer:** Returning only IDs minimizes the payload size and reduces network bandwidth usage. However, it requires the client to make separate requests to fetch the details of each room (the N+1 problem), which increases total latency and server load. Returning full objects is more "chatty" initially but allows the client to display information immediately without additional round-trips, providing a smoother user experience at the cost of higher per-request data usage.

#### 2.2 DELETE Idempotency
**Question:** Is the DELETE operation idempotent in your implementation? Provide a detailed justification.
**Answer:** Yes, the DELETE operation is idempotent. In our implementation, deleting a room for the first time removes it from the map and returns a 200 OK. Sending the same request again will find that the room no longer exists, and our implementation returns a 204 No Content. Since the resulting state of the server (the room being gone) is identical after the first and all subsequent requests, the operation fulfills the core definition of idempotency.

### Part 3: Sensor Operations & Linking

#### 3.1 @Consumes Mismatch
**Question:** Explain the technical consequences if a client attempts to send data in a different format, such as text/plain or application/xml, when @Consumes(MediaType.APPLICATION_JSON) is used.
**Answer:** The @Consumes annotation restricts the method to only accept JSON payloads. If a client sends a different format, the JAX-RS runtime will detect the mismatch between the request's `Content-Type` header and the required media type. It will automatically intercept the request and return an **HTTP 415 Unsupported Media Type** response before the business logic is ever executed, ensuring the system remains robust.

#### 3.2 Filtering: QueryParam vs PathParam
**Question:** Why is the query parameter approach generally considered superior for filtering and searching collections?
**Answer:** Query parameters are optional and used to "modify" the view of a resource collection (adjectives), whereas path parameters identify a specific resource (nouns). For filtering, query parameters are superior because they are composable; you can support multiple filters (e.g., `?type=CO2&status=ACTIVE`) without creating complex and brittle URL patterns. This makes the API more flexible and follows RESTful best practices.

### Part 4: Deep Nesting with Sub-Resources

#### 4.1 Sub-Resource Locator Pattern
**Question:** Discuss the architectural benefits of the Sub-Resource Locator pattern.
**Answer:** This pattern allows us to delegate sub-operations (like sensor readings) to separate, specialized classes. This keeps our main `SensorResource` clean and prevents it from becoming a "God Class" that handles every nested path. It improves readability, allows for better code reuse, and maps logically to the hierarchical nature of the campus data model, making the codebase easier to maintain as it grows.

#### 4.2 Hierarchical Resource Design
**Question:** Why is the path `/sensors/{id}/readings` preferred over a flat `/sensor-readings?sensorId={id}` path?
**Answer:** The hierarchical path represents a clear parent-child relationship. In REST, nested resources indicate that the child (Reading) exists only in the context of the parent (Sensor). A flat path suggests that readings are a top-level entity, which is semantically inaccurate for sensors where data points are bound to a specific hardware instance. This design makes the API more intuitive and self-explanatory.

### Part 5: Error Handling & Logging

#### 5.1 Exception Mapping Benefits
**Question:** How does using JAX-RS `ExceptionMapper` improve the separation of concerns in your application?
**Answer:** `ExceptionMapper` allows for centralized error handling, decoupling business logic from HTTP response concerns. Instead of littering resource methods with try-catch blocks and manual `Response` building, we can throw clean domain exceptions. The mapper ensures that every error of a certain type results in a consistent JSON response format and appropriate HTTP status code globally, improving maintainability and API consistency.

#### 5.2 422 vs 404 Accuracy
**Question:** Why is HTTP 422 more semantically accurate than 404 for missing references in payloads?
**Answer:** A `404 Not Found` implies the requested URL does not exist. However, if the URL is correct but the metadata inside the JSON (like a non-existent `roomId` in a Sensor registration) is logically invalid, the resource at that URL simply cannot process the request. `422 Unprocessable Entity` is more accurate because it signals that the request structure is correct, but the business logic cannot proceed with the provided values.

#### 5.3 Handling Forbidden vs. Conflict
**Question:** Explain the difference between returning a 403 Forbidden and a 409 Conflict in the context of deleting a room.
**Answer:** A `403 Forbidden` indicates the user lacks permission for the action. In contrast, a `409 Conflict` indicates a business-rule violation where the request is authorized but conflicts with the server state. When deleting a room that still has sensors, the action is "conflicting" with data integrity. Returning 409 tells the client the operation failed because of the resource's current state, not a lack of permission.

#### 5.4 Cybersecurity Risks of Stack Traces
**Question:** Explain the risks associated with exposing internal Java stack traces to external API consumers.
**Answer:** Exposing stack traces provides an attacker with a blueprint of the application's internal structure. It reveals package names, class structures, third-party library versions (which may have known CVEs), and even logic flows. An attacker can use this "fingerprinting" information to tailor exploits, identify vulnerable dependencies, or understand the system's internal logic to craft more sophisticated attacks.

#### 5.5 JAX-RS Filters Benefits
**Question:** Why use JAX-RS filters for cross-cutting concerns like logging?
**Answer:** Filters are ideal for cross-cutting concerns because they follow the DRY (Don't Repeat Yourself) principle. Instead of manually adding logging statements to every single method, a filter captures all incoming requests and outgoing responses in one place. This ensures absolute consistency, simplifies the core business logic, and makes the application significantly easier to audit and maintain.
