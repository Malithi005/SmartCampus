# Smart Campus API

This is a RESTful API built using JAX-RS (Jersey) and Grizzly, designed to manage rooms, sensors, and sensor readings in a Smart Campus environment.

## Features
- **Project Bootstrap:** Standalone JAX-RS application using Grizzly embedded server.
- **Discovery Endpoint:** HATEOAS-compliant discovery at the API root.
- **Room Management:** CRUD operations for university rooms with safety logic.
- **Sensor Operations:** Register and filter sensors by type.
- **Sensor Readings:** Nested sub-resource for historical sensor data with validation.
- **Error Handling:** Custom exception mappers for consistent JSON error responses.
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
   The application will be available at: **http://localhost:8080/api/v1**
   (WADL available at: **http://localhost:8080/api/v1/application.wadl**)

---

## Report Questions

**Student Name:** Malithi Nadunika  
**Student ID:** W2153611  

### Part 1: Service Architecture & Setup

#### 1.Project & Application Configuration
**Question:** Explain the default lifecycle of a JAX-RS Resource class. Is a new instance instantiated for every incoming request, or does the runtime treat it as a singleton? Elaborate on how this architectural decision impacts the way you manage and synchronize your in-memory data structures (maps/lists) to prevent data loss or race conditions.

**Answer:** By default, JAX-RS resource classes are **request-scoped**. A new instance is created for every HTTP request and discarded after the response is sent. This means instance variables are not persisted across requests. To maintain data like our room and sensor maps, we must use `static` variables or a `Singleton` lifecycle. Since these static collections (like `ConcurrentHashMap`) are shared across threads handling different requests, we must use thread-safe data structures or explicit synchronization to prevent race conditions and data corruption.

#### 2. The ”Discovery” Endpoint
**Question:** Why is the provision of ”Hypermedia” (links and navigation within responses) considered a hallmark of advanced RESTful design (HATEOAS)? How does it benefit client developers compared to static documentation?

**Answer:** HATEOAS makes the API self-documenting and discoverable. Instead of hardcoding URLs, clients follow links provided in responses. This benefits developers by reducing coupling between the client and server; the server can change its URI structure without breaking clients, as long as the relationship names (rel) remain consistent. It also makes the API easier to explore without constantly referencing static documentation, as the state transitions are guided by the server.

### Part 2: Room Management

#### 1.Room Resource Implementation
**Question:** When returning a list of rooms, what are the implications of returning only IDs versus returning the full room objects? Consider network bandwidth and client side processing.

**Answer:** Returning only IDs minimizes the payload size and reduces network bandwidth usage. However, it requires the client to make separate requests to fetch the details of each room (the N+1 problem), which increases total latency and server load. Returning full objects is more "chatty" initially but allows the client to display information immediately without additional round-trips, providing a smoother user experience at the cost of higher per-request data usage.

#### 2. Room Deletion & Safety Logic 
**Question:** Is the DELETE operation idempotent in your implementation? Provide a detailed justification by describing what happens if a client mistakenly sends the exact same DELETE request for a room multiple times.

**Answer:** Yes, the DELETE operation is idempotent. In our implementation, deleting a room for the first time removes it from the map and returns a 200 OK. Sending the same request again will find that the room no longer exists, and our implementation returns a 204 No Content. Since the resulting state of the server (the room being gone) is identical after the first and all subsequent requests, the operation fulfills the core definition of idempotency.

### Part 3: Sensor Operations & Linking

#### 1.Sensor Resource & Integrity
**Question:** Explain the technical consequences if a client attempts to send data in a different format, such as text/plain or application/xml. How does JAX-RS handle this mismatch?

**Answer:** The @Consumes annotation restricts the method to only accept JSON payloads. If a client sends a different format, the JAX-RS runtime will detect the mismatch between the request's `Content-Type` header and the required media type. It will automatically intercept the request and return an **HTTP 415 Unsupported Media Type** response before the business logic is ever executed, ensuring the system remains robust.

#### 2.Filtered Retrieval & Search 
**Question:** You implemented this filtering using @QueryParam. Contrast this with an alternative design where the type is part of the URL path (e.g., /api/vl/sensors/type/CO2). Why is the query parameter approach generally considered superior for filtering and searching collections?

**Answer:** Query parameters are optional and used to "modify" the view of a resource collection (adjectives), whereas path parameters identify a specific resource (nouns). For filtering, query parameters are superior because they are composable; you can support multiple filters (e.g., `?type=CO2&status=ACTIVE`) without creating complex and brittle URL patterns. This makes the API more flexible and follows RESTful best practices.

### Part 4: Deep Nesting with Sub-Resources

#### 1.The Sub-Resource Locator Pattern
**Question:** Discuss the architectural benefits of the Sub-Resource Locator pattern. How does delegating logic to separate classes help manage complexity in large APIs compared to defining every nested path (e.g., sensors/{id}/readings/{rid}) in one massive controller class?

**Answer:** This pattern allows us to delegate sub-operations (like sensor readings) to separate, specialized classes. This keeps our main `SensorResource` clean and prevents it from becoming a "God Class" that handles every nested path. It improves readability, allows for better code reuse, and maps logically to the hierarchical nature of the campus data model, making the codebase easier to maintain as it grows. The hierarchical path represents a clear parent-child relationship. In REST, nested resources indicate that the child (Reading) exists only in the context of the parent (Sensor). A flat path suggests that readings are a top-level entity, which is semantically inaccurate for sensors where data points are bound to a specific hardware instance. This design makes the API more intuitive and self-explanatory.

### Part 5: Error Handling & Logging

#### 2.Dependency Validation (422 Unprocessable Entity)
**Question:** Why is HTTP 422 often considered more semantically accurate than a standard 404 when the issue is a missing reference inside a valid JSON payload?

**Answer:** A `404 Not Found` implies the requested URL does not exist. However, if the URL is correct but the metadata inside the JSON (like a `roomId`) points to something non-existent, the request is "Unprocessable." `422 Unprocessable Entity` tells the client: "I understood your request structure, but the business logic cannot handle these specific values."

#### 4. The Global Safety Net (500)
**Question:** From a cybersecurity standpoint, explain the risks associated with exposing internal Java stack traces to external API consumers. What specific information could an attacker gather from such a trace?

**Answer:** Exposing stack traces provides an attacker with a blueprint of the application's internal structure. It reveals package names, class structures, third-party library versions (which may have known CVEs), and even logic flows. An attacker can use this "fingerprinting" information to tailor exploits, identify vulnerable dependencies, or understand the system's internal logic to craft more sophisticated attacks.

#### 5.API Request & Response Logging Filters 
**Question:** Why is it advantageous to use JAX-RS filters for cross-cutting concerns like logging, rather than manually inserting Logger.info() statements inside every single resource method?

**Answer:** Filters are ideal for cross-cutting concerns because they follow the DRY (Don't Repeat Yourself) principle. Instead of manually adding logging statements to every single method, a filter captures all incoming requests and outgoing responses in one place. This ensures absolute consistency, simplifies the core business logic, and makes the application significantly easier to audit and maintain.
