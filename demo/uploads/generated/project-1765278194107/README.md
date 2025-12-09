# BookAPI

## Overview
This project was automatically generated from specification files using SpecToCodeAgent.

## Project Structure
- **Entities**: Domain models representing database tables
- **DTOs**: Data Transfer Objects for API requests/responses
- **Repositories**: Data access layer interfaces
- **Services**: Business logic layer
- **Controllers**: REST API endpoints

## Generated Entities
- Book
- Task

## Building and Running

### Prerequisites
- Java 17 or higher
- Maven 3.6+

### Build
```bash
mvn clean install
```

### Run
```bash
mvn spring-boot:run
```

## GAP Report
The following items were identified as missing or ambiguous in the specifications:

- GraphQL schema parsing is not fully implemented - manual review required
