# APEX REST API Versioning Strategy

## Overview

This document outlines the comprehensive API versioning strategy for the APEX REST API, ensuring backward compatibility, clear upgrade paths, and maintainable API evolution.

## Versioning Approach

### 1. Semantic Versioning (SemVer)

The APEX REST API follows **Semantic Versioning 2.0.0** principles:

- **MAJOR.MINOR.PATCH** (e.g., `v2.1.3`)
- **MAJOR**: Breaking changes that require client updates
- **MINOR**: New features that are backward compatible
- **PATCH**: Bug fixes and non-breaking improvements

### 2. URL Path Versioning

**Primary Strategy**: Version is included in the URL path

```
https://api.apex.dev/v1/rules/check
https://api.apex.dev/v2/rules/check
```

**Benefits:**
- Clear and explicit versioning
- Easy to cache and route
- Simple for clients to understand
- RESTful and discoverable

### 3. Header-Based Versioning (Secondary)

**Fallback Strategy**: Version specified in HTTP headers

```http
GET /api/rules/check HTTP/1.1
Host: api.apex.dev
Accept: application/vnd.apex.v1+json
API-Version: v1
```

## Current Version Status

### Version 1.0 (Current)
- **Status**: Active, Stable
- **Base Path**: `/api/` (implicit v1)
- **Supported Until**: TBD (minimum 2 years from v2 release)
- **Features**: Core rule evaluation, validation, configuration management

### Version 2.0 (Planned)
- **Status**: In Development
- **Base Path**: `/api/v2/`
- **Target Release**: Q2 2025
- **New Features**: Enhanced validation, batch processing, async operations

## Versioning Implementation

### 1. URL Structure

```
Current (v1):   /api/rules/check
Future (v2):    /api/v2/rules/check
Future (v3):    /api/v3/rules/check
```

### 2. Version Detection Logic

```java
@RestController
@RequestMapping("/api")  // v1 (default)
public class RulesControllerV1 {
    // v1 implementation
}

@RestController
@RequestMapping("/api/v2")
public class RulesControllerV2 {
    // v2 implementation
}
```

### 3. Content Negotiation

```http
# Request specific version via Accept header
Accept: application/vnd.apex.v1+json
Accept: application/vnd.apex.v2+json

# Default to latest stable version
Accept: application/json
```

## Breaking Changes Policy

### What Constitutes a Breaking Change

1. **Removing endpoints or fields**
2. **Changing field types or formats**
3. **Modifying required parameters**
4. **Changing HTTP status codes for existing scenarios**
5. **Altering authentication/authorization requirements**
6. **Modifying error response structures**

### What Does NOT Constitute a Breaking Change

1. **Adding new optional fields to responses**
2. **Adding new endpoints**
3. **Adding new optional parameters**
4. **Improving error messages (keeping structure)**
5. **Performance improvements**
6. **Bug fixes that don't change behavior**

## Version Lifecycle Management

### 1. Development Phase
- **Duration**: 3-6 months
- **Status**: Alpha/Beta
- **Availability**: Development/staging environments only
- **Documentation**: Draft specifications

### 2. Release Phase
- **Duration**: Ongoing
- **Status**: Stable
- **Availability**: Production
- **Documentation**: Complete API documentation
- **Support**: Full support and maintenance

### 3. Deprecation Phase
- **Duration**: Minimum 12 months
- **Status**: Deprecated but supported
- **Availability**: Production (with warnings)
- **Documentation**: Migration guides provided
- **Support**: Security fixes only

### 4. End-of-Life Phase
- **Status**: Unsupported
- **Availability**: Removed from production
- **Documentation**: Archived
- **Support**: None

## Client Migration Strategy

### 1. Deprecation Warnings

```http
HTTP/1.1 200 OK
Deprecation: true
Sunset: Sat, 31 Dec 2025 23:59:59 GMT
Link: </api/v2/rules/check>; rel="successor-version"
Warning: 299 - "API version v1 is deprecated. Please migrate to v2."

{
  "success": true,
  "apiVersion": "v1",
  "deprecationWarning": {
    "message": "This API version is deprecated",
    "sunsetDate": "2025-12-31T23:59:59Z",
    "migrationGuide": "https://docs.apex.dev/migration/v1-to-v2"
  }
}
```

### 2. Migration Timeline

```
Month 1-3:   v2 Beta available for testing
Month 4:     v2 Production release
Month 5-16:  v1 deprecated but supported
Month 17:    v1 end-of-life
```

## Error Response Versioning

### Version 1.0 Error Format

```json
{
  "success": false,
  "error": "Validation failed",
  "details": "Age must be at least 18",
  "timestamp": "2025-07-31T10:30:00Z"
}
```

### Version 2.0 Error Format (RFC 7807)

```json
{
  "type": "/problems/validation-error",
  "title": "Validation Error",
  "status": 400,
  "detail": "Request validation failed",
  "instance": "/api/v2/rules/validate",
  "timestamp": "2025-07-31T10:30:00Z",
  "correlationId": "abc123-def456",
  "apiVersion": "v2",
  "errors": [
    {
      "field": "age",
      "message": "must be greater than or equal to 18",
      "rejectedValue": 16
    }
  ]
}
```

## Documentation Strategy

### 1. Version-Specific Documentation

- **Separate OpenAPI specs** for each major version
- **Version-specific examples** and tutorials
- **Migration guides** between versions
- **Changelog** with detailed breaking changes

### 2. Documentation URLs

```
v1 Docs:    https://docs.apex.dev/api/v1/
v2 Docs:    https://docs.apex.dev/api/v2/
Migration:  https://docs.apex.dev/migration/v1-to-v2/
```

### 3. Interactive Documentation

```
v1 Swagger:  https://api.apex.dev/swagger-ui.html
v2 Swagger:  https://api.apex.dev/v2/swagger-ui.html
```

## Testing Strategy

### 1. Version Compatibility Testing

- **Automated tests** for each supported version
- **Contract testing** to ensure backward compatibility
- **Integration tests** across version boundaries
- **Performance regression testing**

### 2. Test Organization

```
src/test/java/
├── v1/
│   ├── integration/
│   └── contract/
├── v2/
│   ├── integration/
│   └── contract/
└── migration/
    └── V1ToV2MigrationTest.java
```

## Monitoring and Analytics

### 1. Version Usage Tracking

- **Request metrics** by API version
- **Client adoption rates** for new versions
- **Error rates** by version
- **Performance metrics** comparison

### 2. Deprecation Monitoring

- **Usage alerts** for deprecated versions
- **Client notification** systems
- **Migration progress** tracking

## Implementation Checklist

### For New Major Version Release

- [ ] Create new controller classes with version prefix
- [ ] Update OpenAPI specifications
- [ ] Implement version detection middleware
- [ ] Add deprecation headers to old version
- [ ] Create migration documentation
- [ ] Set up version-specific monitoring
- [ ] Update client SDKs
- [ ] Communicate changes to stakeholders

### For Minor Version Release

- [ ] Ensure backward compatibility
- [ ] Update API documentation
- [ ] Add new feature tests
- [ ] Update changelog
- [ ] Deploy with feature flags if needed

### For Patch Release

- [ ] Verify no breaking changes
- [ ] Update patch version number
- [ ] Deploy bug fixes
- [ ] Update documentation if needed

## Best Practices

### 1. Design for Evolution

- **Use extensible data structures** (avoid arrays for objects)
- **Provide optional fields** with sensible defaults
- **Design consistent error formats**
- **Plan for pagination** from the start

### 2. Communication

- **Announce changes** well in advance
- **Provide clear migration paths**
- **Offer support** during transitions
- **Maintain comprehensive documentation**

### 3. Backward Compatibility

- **Never remove fields** in minor versions
- **Always provide default values** for new required fields
- **Maintain consistent behavior** across versions
- **Use feature flags** for gradual rollouts

## Support and Contact

- **API Team**: api-team@apex.dev
- **Documentation**: https://docs.apex.dev
- **Support Portal**: https://support.apex.dev
- **Migration Assistance**: migration-help@apex.dev

---

**Last Updated**: July 31, 2025  
**Version**: 1.0  
**Next Review**: October 31, 2025
