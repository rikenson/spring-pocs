# OpenAPI 3.0.3 Specifications

This directory contains the modular OpenAPI 3.0.3 specifications for the Client Management API, featuring modern design patterns, schema inheritance, and comprehensive documentation.

## Table of Contents

- [Overview](#overview)
- [Structure](#structure)
- [Features](#features)
- [Schema Design](#schema-design)
- [Build Process](#build-process)
- [Usage](#usage)
- [Best Practices](#best-practices)
- [Troubleshooting](#troubleshooting)

## Overview

The API specifications are designed using a modular approach with OpenAPI 3.0.3, providing:

- **Schema Inheritance**: Reduces duplication using `allOf` patterns
- **Component Reuse**: Shared parameters, responses, and security schemes
- **Type Safety**: Strong typing with proper validation constraints
- **Documentation**: Comprehensive descriptions and examples
- **Standards Compliance**: Follows OpenAPI 3.0.3 best practices

## Structure

```
specs/
├── index.yml                    # Main API specification entry point
├── build.sh                     # Build script to generate bundled spec
├── oas3.yaml                    # Generated bundled specification
├── schemas/                     # Schema definitions
│   ├── client.yml              # Client domain schemas with inheritance
│   ├── base-models.yml         # Base model definitions (audit, pagination)
│   └── errors.yml              # Standardized error response schemas
├── paths/                      # API endpoint definitions
│   └── clients.yml             # Client management endpoints
└── params/                     # Reusable parameter definitions
    └── common.yml              # Common query parameters and headers
```

## Features

### Modern OpenAPI 3.0.3 Features

- ✅ **OpenAPI 3.0.3 Compliance**: Latest specification version
- ✅ **Server Variables**: Flexible environment configuration
- ✅ **Schema Inheritance**: `allOf` patterns for DRY principles
- ✅ **Component References**: Reusable components across modules
- ✅ **Security Schemes**: OAuth2, JWT, and API key support
- ✅ **Request/Response Examples**: Comprehensive example data
- ✅ **Validation Constraints**: Proper type validation and constraints

### Design Patterns

#### 1. Schema Inheritance
```yaml
# Base schema
ClientBase:
  type: object
  properties:
    id:
      type: integer
      format: int64
    clientName:
      type: string
    # ... other common properties

# Extended schema  
ClientSummaryDto:
  allOf:
    - $ref: '#/ClientBase'
    - type: object
      required:
        - id
        - clientName
      properties:
        version:
          type: integer
          format: int32
        # ... additional properties
```

#### 2. Component Reuse
```yaml
components:
  responses:
    BadRequest:
      description: Bad request - Invalid input parameters
      content:
        application/json:
          schema:
            $ref: \"./schemas/errors.yml#/ErrorResponse\"
    
  parameters:
    PageParameter:
      $ref: \"./params/common.yml#/page\"
    
  securitySchemes:
    BearerAuth:
      type: http
      scheme: bearer
      bearerFormat: JWT
```

#### 3. Path Organization
```yaml
paths:
  /clients/search:
    $ref: \"./paths/clients.yml#/search\"
  /clients/search-one:
    $ref: \"./paths/clients.yml#/searchOne\"
  /clients/{id}:
    $ref: \"./paths/clients.yml#/byId\"
```

## Schema Design

### Base Models (base-models.yml)

#### IDModel
```yaml
IDModel:
  type: object
  required:
    - currentId
  properties:
    currentId:
      type: integer
      format: int64
      example: 1
```

#### AuditModel
```yaml
AuditModel:
  type: object
  required:
    - createdAt
    - modifiedAt
    - createdBy
    - modifiedBy
  properties:
    createdAt:
      type: string
      format: date-time
      description: Timestamp when the resource was created
    modifiedAt:
      type: string
      format: date-time
      description: Timestamp when the resource was last modified
    createdBy:
      type: string
      description: ID of the user who created the resource
    modifiedBy:
      type: string
      description: ID of the user who last modified the resource
```

#### Pagination Support
```yaml
PageableExtra:
  type: object
  required:
    - totalElements
    - totalPages
    - size
    - number
    - first
    - last
    - empty
  properties:
    totalElements:
      type: integer
      format: int64
      description: Total number of elements
    totalPages:
      type: integer
      format: int64
      description: Total number of pages
    # ... other pagination properties
```

### Client Domain (client.yml)

#### Inheritance Hierarchy
```yaml
# Base schema with common properties
ClientBase:
  type: object
  properties:
    id:
      type: integer
      format: int64
      description: \"Unique client identifier\"
    clientName:
      type: string
      description: \"Client name\"
    clientNumber:
      type: string
      description: \"Unique client number\"
    # ... other shared properties

# DTO extending base
ClientSummaryDto:
  allOf:
    - $ref: '#/ClientBase'
    - type: object
      required:
        - id
        - clientName
        - clientNumber
      properties:
        version:
          type: integer
          format: int32
        externalReferences:
          type: array
          items:
            $ref: '#/ExternalReferenceEntity'

# Filter extending base  
ClientSummaryFilter:
  allOf:
    - $ref: '#/ClientBase'
    - type: object
      properties:
        # Override descriptions for filter context
        clientName:
          type: string
          description: \"Filter by client name (partial match)\"
        # Additional filter-specific properties
        page:
          type: integer
          format: int32
          default: 0
          minimum: 0
        size:
          type: integer
          format: int32
          default: 20
          minimum: 1
          maximum: 100
```

### Error Handling (errors.yml)

#### Standardized Error Response
```yaml
ErrorResponse:
  type: object
  required:
    - timestamp
    - status
    - error
    - message
    - path
  properties:
    timestamp:
      type: string
      format: date-time
      description: Time when the error occurred
    status:
      type: integer
      format: int32
      description: HTTP status code
    error:
      type: string
      description: HTTP status text
    message:
      type: string
      description: Detailed error message
    path:
      type: string
      description: API path where the error occurred
    details:
      type: array
      items:
        type: string
      description: Additional error details
```

## Build Process

### Automatic Building

The specifications are automatically built during Maven compilation:

```bash
# Maven build triggers OpenAPI generation
mvn compile

# Manual build
cd src/main/resources/specs
./build.sh
```

### Build Script (build.sh)

```bash
#!/bin/bash
# Bundle the complete spec using Redocly CLI
redocly bundle index.yml -o ./oas3.yaml
```

### Generated Artifacts

1. **oas3.yaml**: Bundled specification file
2. **Java Classes**: Generated model and API classes
   - Location: `target/generated-sources/openapi/`
   - Package: `com.tiger.pocs.payload` and `com.tiger.pocs.handler`

### Maven Integration

```xml
<plugin>
    <groupId>org.codehaus.mojo</groupId>
    <artifactId>exec-maven-plugin</artifactId>
    <executions>
        <execution>
            <id>generate-openapi-spec</id>
            <phase>generate-sources</phase>
            <goals>
                <goal>exec</goal>
            </goals>
            <configuration>
                <executable>bash</executable>
                <workingDirectory>${project.basedir}/src/main/resources/specs</workingDirectory>
                <arguments>
                    <argument>build.sh</argument>
                </arguments>
            </configuration>
        </execution>
    </executions>
</plugin>
```

## Usage

### Accessing the API Documentation

1. **Swagger UI**: http://localhost:8080/swagger-ui.html
2. **OpenAPI JSON**: http://localhost:8080/v3/api-docs
3. **Raw Specification**: View `oas3.yaml` file

### Client Code Generation

Generate client SDKs in various languages:

```bash
# Generate Java client
openapi-generator-cli generate \
  -i oas3.yaml \
  -g java \
  -o generated-clients/java \
  --additional-properties=dateLibrary=java8

# Generate TypeScript client
openapi-generator-cli generate \
  -i oas3.yaml \
  -g typescript-fetch \
  -o generated-clients/typescript

# Generate Python client
openapi-generator-cli generate \
  -i oas3.yaml \
  -g python \
  -o generated-clients/python
```

### Schema Validation

```bash
# Validate specification
redocly lint index.yml

# Bundle and validate
redocly bundle index.yml --lint
```

## Best Practices

### 1. Schema Design

#### Use Inheritance Wisely
```yaml
# ✅ Good: Common base for related schemas
ClientBase:
  type: object
  properties:
    id: { type: integer }
    name: { type: string }

ClientDto:
  allOf:
    - $ref: '#/ClientBase'
    - type: object
      properties:
        additionalField: { type: string }

# ❌ Avoid: Unnecessary inheritance
SimpleModel:
  allOf:
    - type: object
      properties:
        onlyField: { type: string }
```

#### Proper Validation
```yaml
# ✅ Good: Comprehensive validation
clientName:
  type: string
  minLength: 1
  maxLength: 100
  pattern: \"^[A-Za-z0-9\\s\\-_]+$\"
  description: \"Client name with alphanumeric characters only\"

# ❌ Avoid: Missing validation
clientName:
  type: string
```

### 2. Component Organization

#### File Structure
- Keep related schemas in the same file
- Use clear, descriptive names
- Group by domain/functionality
- Maintain consistent naming conventions

#### Reference Patterns
```yaml
# ✅ Good: Clear reference paths
$ref: \"./schemas/client.yml#/ClientSummaryDto\"
$ref: \"../responses.yml#/BadRequest\"

# ❌ Avoid: Complex nested references
$ref: \"../../other/deeply/nested/file.yml#/SomeSchema\"
```

### 3. Documentation

#### Descriptions
```yaml
# ✅ Good: Clear, helpful descriptions
properties:
  clientNumber:
    type: string
    description: \"Unique identifier for the client, following format CLI-XXXXXX\"
    example: \"CLI-001234\"
    pattern: \"^CLI-[0-9]{6}$\"

# ❌ Avoid: Obvious or missing descriptions
properties:
  id:
    type: integer
    description: \"The ID\"
```

#### Examples
```yaml
# ✅ Good: Realistic examples
example:
  id: 123
  clientName: \"Acme Corporation\"
  clientNumber: \"CLI-001234\"
  deleted: false

# ❌ Avoid: Placeholder examples
example:
  id: 1
  clientName: \"string\"
  deleted: true
```

### 4. Error Handling

#### Consistent Error Responses
```yaml
responses:
  '400':
    $ref: '#/components/responses/BadRequest'
  '404':
    $ref: '#/components/responses/NotFound'
  '500':
    $ref: '#/components/responses/InternalServerError'
```

#### Error Schema Design
```yaml
ErrorResponse:
  type: object
  required: [timestamp, status, error, message, path]
  properties:
    timestamp: { type: string, format: date-time }
    status: { type: integer, format: int32 }
    error: { type: string }
    message: { type: string }
    path: { type: string }
    details:
      type: array
      items: { type: string }
```

## Troubleshooting

### Common Issues

#### 1. Build Failures

**Issue**: `redocly bundle` command fails
```bash
Error: Cannot resolve reference: ./schemas/missing.yml#/Schema
```

**Solution**: Check file paths and schema references
```bash
# Verify file exists
ls -la schemas/

# Check reference syntax
grep -r \"missing.yml\" .
```

#### 2. Circular References

**Issue**: Infinite loops in schema references
```bash
Error: Detected circular reference
```

**Solution**: Break circular dependencies
```yaml
# ❌ Problem: Circular reference
ClientDto:
  allOf:
    - $ref: '#/BaseDto'
    - properties:
        relatedClient:
          $ref: '#/ClientDto'  # Circular!

# ✅ Solution: Use ID reference
ClientDto:
  allOf:
    - $ref: '#/BaseDto'
    - properties:
        relatedClientId:
          type: integer
          format: int64
```

#### 3. Generation Warnings

**Issue**: \"Ignoring complex example on request body\"
```bash
[WARNING] Ignoring complex example on request body
```

**Solution**: Simplify examples or use single `example` instead of `examples`
```yaml
# ✅ Simple example
requestBody:
  content:
    application/json:
      schema:
        $ref: '#/components/schemas/ClientFilter'
      example:
        clientName: \"Acme\"
        page: 0
        size: 20

# ❌ Complex examples causing warnings
requestBody:
  content:
    application/json:
      schema:
        $ref: '#/components/schemas/ClientFilter'
      examples:
        basic:
          summary: \"Basic search\"
          value:
            clientName: \"Acme\"
```

#### 4. Maven Build Issues

**Issue**: OpenAPI generation fails during build
```bash
[ERROR] Failed to execute goal org.openapitools:openapi-generator-maven-plugin
```

**Solution**: Check Maven plugin configuration and dependencies
```xml
<!-- Ensure proper plugin version -->
<plugin>
    <groupId>org.openapitools</groupId>
    <artifactId>openapi-generator-maven-plugin</artifactId>
    <version>7.0.0</version>
    <!-- ... configuration ... -->
</plugin>
```

### Debugging Steps

1. **Validate Specification**:
   ```bash
   redocly lint index.yml
   ```

2. **Check References**:
   ```bash
   grep -r \"\\$ref\" . | grep -v oas3.yaml
   ```

3. **Manual Bundle**:
   ```bash
   cd src/main/resources/specs
   ./build.sh
   cat oas3.yaml | head -50
   ```

4. **Generator Logs**:
   ```bash
   mvn compile -X | grep -A 10 -B 10 openapi-generator
   ```

### Performance Optimization

#### Bundle Size Optimization
- Remove unused schemas and parameters
- Minimize deeply nested inheritance
- Use references instead of inline definitions
- Compress large examples

#### Build Time Optimization
- Cache generated files when possible
- Use incremental builds
- Parallelize generation tasks
- Optimize file watching in development

---

## Contributing

When modifying the API specifications:

1. **Follow naming conventions**: PascalCase for schemas, camelCase for properties
2. **Update examples**: Ensure all examples are realistic and helpful
3. **Validate changes**: Run `redocly lint` before committing
4. **Test generation**: Verify Maven build succeeds
5. **Update documentation**: Keep this README current

## Resources

- **OpenAPI 3.0.3 Specification**: https://spec.openapis.org/oas/v3.0.3
- **Redocly CLI**: https://redocly.com/docs/cli/
- **OpenAPI Generator**: https://openapi-generator.tech/
- **JSON Schema Validation**: https://json-schema.org/

For questions or issues with the API specifications, refer to the main project README or create an issue in the project repository.