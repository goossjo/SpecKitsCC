# Implementing Specification-Driven Development (SDD)
To ensure consistency across our codebase when using agentic tools, we follow a Specification-Driven Development workflow. This involves defining requirements in structured .spec.md files before any code is generated.

## 1. Repository Configuration
All prompts and specifications are version-controlled within the repository. This provides an audit trail of architectural decisions.

- **Location:** .github/prompts/

- **Naming Convention:** Use the suffix *.spec.md (e.g., user-service-auth.spec.md).

## 2. Defining Coding Standards
A high-quality specification must include a Standards section. This prevents the agent from making assumptions that deviate from our internal patterns.

## 3. Using the spec
When prompting try to reference the spec.

Example: 

> "Review the coding standards in .github/prompts/coding-standards.spec.md and implement an IBAN 97 checksum validator. Ensure the implementation follows our specific rules for exception handling and unit testing as defined in the spec."

### Example Standard Template:

```markdown
# 📝 Sping Coding Specification

# Coding Standard

### Additional Coding Standards

1. **Package Naming:** Use Java best practices for package naming (all lowercase, domain/feature-based structure).
2. ....

### Rule: Naming Conventions

1.  **Interface Naming:** Interfaces MUST be suffixed with `Service` (e.g., `UserService`).
2.  **Implementation Naming:** Implementation classes MUST be suffixed with `ServiceImpl` (e.g., `UserServiceImpl`).
3.  **Variable Scope:** Local final variables must use `camelCase`.

### Rule: Repository Naming Conventions

1.  ...

### Rule: Whitespace and Imports

1.  **Indentation:** Use 4 spaces for indentation everywhere (no tabs).
2. ...


### Spring-Specific Rules

1. **Autowired Fields:** Prefer constructor injection over field injection for `@Autowired` dependencies.
2. ...

## Test Writing Rules

1. **Rule Coverage:** Each test must validate a specific rule from the coding specification above.
2. ...

```

## 4. Findings

To make these findings sound more professional and insightful, we should frame them as **"Key Observations"** or **"Developer Experience (DX) Insights."** This shifts the tone from a simple list of pros and cons to a structured evaluation of the workflow.

---

### Evaluation: The Impact of Spec-Driven Development

After integrating Agentic workflows with a centralized `.github/prompts` directory, we’ve identified several key takeaways regarding efficiency and reliability.

---

### The Advantages: Scalable Consistency & Team Alignment

The primary benefit of this approach is the **reduction of cognitive load** on the developer, alongside a significant "soft" benefit for team culture:

* **Prompt Efficiency:** By offloading coding standards to a permanent spec file, we eliminate "prompt bloat." You no longer need to repeat boilerplate instructions (e.g., "use JUnit 5," "follow Hexagonal architecture") in every interaction.
* **Enforced Uniformity:** It ensures that every team member, regardless of their personal prompting style, generates code that aligns with the project’s specific architectural guardrails.
* **Cultural Alignment (The "Common Ground" Effect):** Beyond the AI benefits, the act of writing a spec forces the team to sit down and formally agree on conventions. This process surface-levels unspoken assumptions, resolves technical disagreements, and establishes a unified "team voice" that improves manual coding just as much as AI-generated code.

---

### The Challenges: Context Drifting & Cache Issues

While the workflow is powerful, we observed two specific limitations in how agents interact with file-based specs:

* **Update Lag (Stale Context):** When a spec file is updated, agents occasionally rely on a cached version of the previous document. This can lead to the agent following outdated rules even after the `.spec.md` has been modified.
* **Constraint Evasion:** No matter how strict the spec is, agents may still prioritize general training data over your specific instructions if the prompt is too complex.

### Recommended Safeguards

To mitigate these risks, we suggest a **"Trust but Verify"** protocol:

1. **Reflective Prompting:** Ask the agent to list the specific rules it applied from the spec before it generates the code (e.g., *"Which architectural rules from the spec did you apply here?"*).
2. **Explicit Refreshing:** If you update a spec, explicitly tell the agent: *"The spec file has changed; please re-read .github/prompts/coding-standards.spec.md before proceeding."*
3. **Mandatory Review:** Treat AI-generated code as a draft. Always perform a manual diff against the spec to ensure no architectural "shortcuts" were taken.

---

#### Comparison of Workflows

| Feature | Standard Prompting | Spec-Driven (SDD) |
| --- | --- | --- |
| **Setup Time** | Fast | Moderate (Initial spec creation) |
| **Consistency** | Low (Varies per prompt) | High (Standardized across team) |
| **Maintenance** | Manual repetition | Version-controlled updates |
| **Error Rate** | High (Human error in prompts) | Low (Context-aware) |
