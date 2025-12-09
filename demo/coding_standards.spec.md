# 📝 Sping Coding Specification


# Coding Standard

### Additional Coding Standards

1. **Package Naming:** Use Java best practices for package naming (all lowercase, domain/feature-based structure).
2. **Field Visibility:** Fields in classes must be private by default.
3. **Class Member Order:** Order class members as follows: fields, constructors, public methods, private methods.
4. **Lombok:** Avoid Lombok annotations (e.g., @Getter, @Setter, @Data).
5. **Static Usage:** No restrictions on static methods or fields.
6. **Final Parameters:** Declaring method parameters as final is optional.
7. **Exception Handling:** Use checked and unchecked exceptions as appropriate.
8. **Custom Exceptions:** Custom exceptions must extend RuntimeException.
9. **Utility Classes:** Utility/helper classes are allowed and must end with `Utils`.
10. **Generics:** Always use explicit type arguments for collections and generics (no raw types).
11. **Logging:** Use SLF4J for logging.
12. **Magic Numbers:** Avoid magic numbers; use named constants instead.
13. **Test Naming:** Test method names must use the pattern `given...should...`.
14. **Dependency Injection:** Use constructor injection for all services and repositories.
15. **Reflection/Proxies:** No restrictions on reflection or dynamic proxies.
16. **DTOs:** All API responses must use DTOs, but do not add 'DTO' to the class name.
17. **Null Handling:** Use `Optional` for handling null values.
18. **Builder Pattern:** Use builder pattern for DTOs.
19. **Method Length/Complexity:** No restrictions on method length or cyclomatic complexity.

### Rule: Naming Conventions

1.  **Interface Naming:** Interfaces MUST be suffixed with `Service` (e.g., `UserService`).
2.  **Implementation Naming:** Implementation classes MUST be suffixed with `ServiceImpl` (e.g., `UserServiceImpl`).
3.  **Variable Scope:** Local final variables must use `camelCase`.

### Rule: Repository Naming Conventions

1.  **Interface Naming:** Repository interfaces MUST be suffixed with `Repository` (e.g., `UserRepository`).
2.  **Implementation Naming:** Implementation classes MUST be suffixed with `RepositoryImpl` (e.g., `UserRepositoryImpl`).
3.  **Variable Scope:** Local final variables in repository classes must use `camelCase`.

### Rule: Whitespace and Imports

1.  **Indentation:** Use 4 spaces for indentation everywhere (no tabs).
2.  **Wildcard Imports:** Wildcard imports (`import java.util.*`) are strictly FORBIDDEN.


### Spring-Specific Rules

1. **Autowired Fields:** Prefer constructor injection over field injection for `@Autowired` dependencies.
2. **Component Naming:** Custom components must be annotated with `@Service`, `@Repository`, or `@Component` as appropriate.
3. **Transactional Methods:** Methods that modify data must be annotated with `@Transactional`.
4. **Configuration Classes:** Configuration classes must be annotated with `@Configuration` and avoid component scanning inside configuration classes.
5. **Exception Handling:** Use `@ControllerAdvice` for centralized exception handling in controllers`

## Test Writing Rules

1. **Rule Coverage:** Each test must validate a specific rule from the coding specification above.
2. **Naming:** Test names must clearly indicate the rule being tested (e.g., `testInterfaceNamingConvention`).
3. **Positive & Negative Cases:** For each rule, provide at least one passing (compliant) and one failing (non-compliant) example.
4. **Isolation:** Each test should focus on a single rule to avoid ambiguity.
5. **Clarity:** Use clear and concise code snippets in tests to illustrate compliance or violation.
6. **Spring Context:** For Spring-specific rules, tests should use minimal Spring annotations and structure to demonstrate the rule.
7. **Whitespace & Imports:** Include tests for indentation and import statements, showing both correct and incorrect usage.
8. **Transactional Methods:** Provide examples of methods that modify data with and without `@Transactional`.
9. **Exception Handling:** Include controller classes with and without `@ControllerAdvice` for centralized exception handling.
10. **Configuration Classes:** Test for proper use of `@Configuration` and absence of component scanning.

## Example Test Cases

### 1. Interface Naming Convention

**Compliant:**
