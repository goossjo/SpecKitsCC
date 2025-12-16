package cc.spec;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/**
 * Generates code artifacts (entities, DTOs, repositories, services, controllers, tests)
 */
public class CodeGenerator {
    private final Path outputBasePath;
    private final String packageName;

    public CodeGenerator(Path outputBasePath, String packageName) {
        this.outputBasePath = outputBasePath;
        this.packageName = packageName;
    }

    /**
     * Generates a JPA entity class
     */
    public void generateEntity(SpecParser.EntityInfo entity) throws IOException {
        String className = entity.getName();
        StringBuilder code = new StringBuilder();
        code.append("package ").append(packageName).append(".entity;\n\n");
        code.append("import jakarta.persistence.*;\n");
        code.append("import java.util.Objects;\n\n");
        code.append("@Entity\n");
        code.append("@Table(name = \"").append(className.toLowerCase()).append("s\")\n");
        code.append("public class ").append(className).append(" {\n\n");
        code.append("    @Id\n");
        code.append("    @GeneratedValue(strategy = GenerationType.IDENTITY)\n");
        code.append("    private Long id;\n\n");
        
        for (Map.Entry<String, String> field : entity.getFields().entrySet()) {
            String fieldName = field.getKey();
            String fieldType = field.getValue();
            if (!fieldName.equalsIgnoreCase("id")) {
                code.append("    private ").append(fieldType).append(" ").append(fieldName).append(";\n\n");
            }
        }
        
        // Getters and setters
        code.append("    // Getters and Setters\n");
        code.append("    public Long getId() { return id; }\n");
        code.append("    public void setId(Long id) { this.id = id; }\n\n");
        
        for (Map.Entry<String, String> field : entity.getFields().entrySet()) {
            String fieldName = field.getKey();
            String fieldType = field.getValue();
            if (!fieldName.equalsIgnoreCase("id")) {
                String capitalized = capitalize(fieldName);
                code.append("    public ").append(fieldType).append(" get").append(capitalized).append("() { return ").append(fieldName).append("; }\n");
                code.append("    public void set").append(capitalized).append("(").append(fieldType).append(" ").append(fieldName).append(") { this.").append(fieldName).append(" = ").append(fieldName).append("; }\n\n");
            }
        }
        
        code.append("    @Override\n");
        code.append("    public boolean equals(Object o) {\n");
        code.append("        if (this == o) return true;\n");
        code.append("        if (o == null || getClass() != o.getClass()) return false;\n");
        code.append("        ").append(className).append(" that = (").append(className).append(") o;\n");
        code.append("        return Objects.equals(id, that.id);\n");
        code.append("    }\n\n");
        code.append("    @Override\n");
        code.append("    public int hashCode() {\n");
        code.append("        return Objects.hash(id);\n");
        code.append("    }\n");
        code.append("}\n");
        
        writeFile("src/main/java/" + packageName.replace(".", "/") + "/entity/" + className + ".java", code.toString());
    }

    /**
     * Generates a DTO class
     */
    public void generateDTO(SpecParser.EntityInfo entity) throws IOException {
        String className = entity.getName() + "DTO";
        StringBuilder code = new StringBuilder();
        code.append("package ").append(packageName).append(".dto;\n\n");
        code.append("public class ").append(className).append(" {\n\n");
        
        for (Map.Entry<String, String> field : entity.getFields().entrySet()) {
            String fieldName = field.getKey();
            String fieldType = field.getValue();
            code.append("    private ").append(fieldType).append(" ").append(fieldName).append(";\n");
        }
        code.append("\n");
        
        // Getters and setters
        for (Map.Entry<String, String> field : entity.getFields().entrySet()) {
            String fieldName = field.getKey();
            String fieldType = field.getValue();
            String capitalized = capitalize(fieldName);
            code.append("    public ").append(fieldType).append(" get").append(capitalized).append("() { return ").append(fieldName).append("; }\n");
            code.append("    public void set").append(capitalized).append("(").append(fieldType).append(" ").append(fieldName).append(") { this.").append(fieldName).append(" = ").append(fieldName).append("; }\n\n");
        }
        
        code.append("}\n");
        
        writeFile("src/main/java/" + packageName.replace(".", "/") + "/dto/" + className + ".java", code.toString());
    }

    /**
     * Generates a Spring Data JPA repository interface
     */
    public void generateRepository(SpecParser.EntityInfo entity) throws IOException {
        String className = entity.getName() + "Repository";
        String entityName = entity.getName();
        StringBuilder code = new StringBuilder();
        code.append("package ").append(packageName).append(".repository;\n\n");
        code.append("import ").append(packageName).append(".entity.").append(entityName).append(";\n");
        code.append("import org.springframework.data.jpa.repository.JpaRepository;\n");
        code.append("import org.springframework.stereotype.Repository;\n\n");
        code.append("@Repository\n");
        code.append("public interface ").append(className).append(" extends JpaRepository<").append(entityName).append(", Long> {\n");
        code.append("}\n");
        
        writeFile("src/main/java/" + packageName.replace(".", "/") + "/repository/" + className + ".java", code.toString());
    }

    /**
     * Generates a service interface
     */
    public void generateServiceInterface(SpecParser.EntityInfo entity) throws IOException {
        String className = entity.getName() + "Service";
        String entityName = entity.getName();
        String dtoName = entityName + "DTO";
        StringBuilder code = new StringBuilder();
        code.append("package ").append(packageName).append(".service;\n\n");
        code.append("import ").append(packageName).append(".dto.").append(dtoName).append(";\n");
        code.append("import java.util.List;\n\n");
        code.append("public interface ").append(className).append(" {\n");
        code.append("    List<").append(dtoName).append("> findAll();\n");
        code.append("    ").append(dtoName).append(" findById(Long id);\n");
        code.append("    ").append(dtoName).append(" save(").append(dtoName).append(" dto);\n");
        code.append("    ").append(dtoName).append(" update(Long id, ").append(dtoName).append(" dto);\n");
        code.append("    void deleteById(Long id);\n");
        code.append("}\n");
        
        writeFile("src/main/java/" + packageName.replace(".", "/") + "/service/" + className + ".java", code.toString());
    }

    /**
     * Generates a service implementation
     */
    public void generateServiceImpl(SpecParser.EntityInfo entity) throws IOException {
        String className = entity.getName() + "ServiceImpl";
        String interfaceName = entity.getName() + "Service";
        String entityName = entity.getName();
        String dtoName = entityName + "DTO";
        String repoName = entityName + "Repository";
        StringBuilder code = new StringBuilder();
        code.append("package ").append(packageName).append(".service;\n\n");
        code.append("import ").append(packageName).append(".dto.").append(dtoName).append(";\n");
        code.append("import ").append(packageName).append(".entity.").append(entityName).append(";\n");
        code.append("import ").append(packageName).append(".repository.").append(repoName).append(";\n");
        code.append("import org.springframework.beans.factory.annotation.Autowired;\n");
        code.append("import org.springframework.stereotype.Service;\n");
        code.append("import java.util.List;\n");
        code.append("import java.util.stream.Collectors;\n\n");
        code.append("@Service\n");
        code.append("public class ").append(className).append(" implements ").append(interfaceName).append(" {\n\n");
        code.append("    @Autowired\n");
        code.append("    private ").append(repoName).append(" repository;\n\n");
        code.append("    @Override\n");
        code.append("    public List<").append(dtoName).append("> findAll() {\n");
        code.append("        return repository.findAll().stream()\n");
        code.append("                .map(this::toDTO)\n");
        code.append("                .collect(Collectors.toList());\n");
        code.append("    }\n\n");
        code.append("    @Override\n");
        code.append("    public ").append(dtoName).append(" findById(Long id) {\n");
        code.append("        return repository.findById(id)\n");
        code.append("                .map(this::toDTO)\n");
        code.append("                .orElse(null);\n");
        code.append("    }\n\n");
        code.append("    @Override\n");
        code.append("    public ").append(dtoName).append(" save(").append(dtoName).append(" dto) {\n");
        code.append("        ").append(entityName).append(" entity = toEntity(dto);\n");
        code.append("        return toDTO(repository.save(entity));\n");
        code.append("    }\n\n");
        code.append("    @Override\n");
        code.append("    public ").append(dtoName).append(" update(Long id, ").append(dtoName).append(" dto) {\n");
        code.append("        ").append(entityName).append(" entity = repository.findById(id)\n");
        code.append("                .orElseThrow(() -> new RuntimeException(\"Entity not found\"));\n");
        code.append("        updateEntityFromDTO(entity, dto);\n");
        code.append("        return toDTO(repository.save(entity));\n");
        code.append("    }\n\n");
        code.append("    @Override\n");
        code.append("    public void deleteById(Long id) {\n");
        code.append("        repository.deleteById(id);\n");
        code.append("    }\n\n");
        code.append("    private ").append(dtoName).append(" toDTO(").append(entityName).append(" entity) {\n");
        code.append("        ").append(dtoName).append(" dto = new ").append(dtoName).append("();\n");
        for (Map.Entry<String, String> field : entity.getFields().entrySet()) {
            String fieldName = field.getKey();
            String capitalized = capitalize(fieldName);
            code.append("        dto.set").append(capitalized).append("(entity.get").append(capitalized).append("());\n");
        }
        code.append("        return dto;\n");
        code.append("    }\n\n");
        code.append("    private ").append(entityName).append(" toEntity(").append(dtoName).append(" dto) {\n");
        code.append("        ").append(entityName).append(" entity = new ").append(entityName).append("();\n");
        for (Map.Entry<String, String> field : entity.getFields().entrySet()) {
            String fieldName = field.getKey();
            if (!fieldName.equalsIgnoreCase("id")) {
                String capitalized = capitalize(fieldName);
                code.append("        entity.set").append(capitalized).append("(dto.get").append(capitalized).append("());\n");
            }
        }
        code.append("        return entity;\n");
        code.append("    }\n\n");
        code.append("    private void updateEntityFromDTO(").append(entityName).append(" entity, ").append(dtoName).append(" dto) {\n");
        for (Map.Entry<String, String> field : entity.getFields().entrySet()) {
            String fieldName = field.getKey();
            if (!fieldName.equalsIgnoreCase("id")) {
                String capitalized = capitalize(fieldName);
                code.append("        entity.set").append(capitalized).append("(dto.get").append(capitalized).append("());\n");
            }
        }
        code.append("    }\n");
        code.append("}\n");
        
        writeFile("src/main/java/" + packageName.replace(".", "/") + "/service/" + className + ".java", code.toString());
    }

    /**
     * Generates a REST controller
     */
    public void generateController(SpecParser.EntityInfo entity, List<SpecParser.EndpointInfo> endpoints) throws IOException {
        String className = entity.getName() + "Controller";
        String entityName = entity.getName();
        String serviceName = entityName + "Service";
        String dtoName = entityName + "DTO";
        StringBuilder code = new StringBuilder();
        code.append("package ").append(packageName).append(".controller;\n\n");
        code.append("import ").append(packageName).append(".dto.").append(dtoName).append(";\n");
        code.append("import ").append(packageName).append(".service.").append(serviceName).append(";\n");
        code.append("import org.springframework.beans.factory.annotation.Autowired;\n");
        code.append("import org.springframework.http.HttpStatus;\n");
        code.append("import org.springframework.http.ResponseEntity;\n");
        code.append("import org.springframework.web.bind.annotation.*;\n");
        code.append("import java.util.List;\n\n");
        code.append("@RestController\n");
        code.append("@RequestMapping(\"/api/").append(entityName.toLowerCase()).append("s\")\n");
        code.append("public class ").append(className).append(" {\n\n");
        code.append("    @Autowired\n");
        code.append("    private ").append(serviceName).append(" service;\n\n");
        code.append("    @GetMapping\n");
        code.append("    public ResponseEntity<List<").append(dtoName).append(">> getAll() {\n");
        code.append("        return ResponseEntity.ok(service.findAll());\n");
        code.append("    }\n\n");
        code.append("    @GetMapping(\"/{id}\")\n");
        code.append("    public ResponseEntity<").append(dtoName).append("> getById(@PathVariable Long id) {\n");
        code.append("        ").append(dtoName).append(" dto = service.findById(id);\n");
        code.append("        return dto != null ? ResponseEntity.ok(dto) : ResponseEntity.notFound().build();\n");
        code.append("    }\n\n");
        code.append("    @PostMapping\n");
        code.append("    public ResponseEntity<").append(dtoName).append("> create(@RequestBody ").append(dtoName).append(" dto) {\n");
        code.append("        return ResponseEntity.status(HttpStatus.CREATED).body(service.save(dto));\n");
        code.append("    }\n\n");
        code.append("    @PutMapping(\"/{id}\")\n");
        code.append("    public ResponseEntity<").append(dtoName).append("> update(@PathVariable Long id, @RequestBody ").append(dtoName).append(" dto) {\n");
        code.append("        return ResponseEntity.ok(service.update(id, dto));\n");
        code.append("    }\n\n");
        code.append("    @DeleteMapping(\"/{id}\")\n");
        code.append("    public ResponseEntity<Void> delete(@PathVariable Long id) {\n");
        code.append("        service.deleteById(id);\n");
        code.append("        return ResponseEntity.noContent().build();\n");
        code.append("    }\n");
        code.append("}\n");
        
        writeFile("src/main/java/" + packageName.replace(".", "/") + "/controller/" + className + ".java", code.toString());
    }

    /**
     * Generates a test class
     */
    public void generateTest(SpecParser.EntityInfo entity) throws IOException {
        String className = entity.getName() + "ControllerTest";
        String entityName = entity.getName();
        String controllerName = entityName + "Controller";
        StringBuilder code = new StringBuilder();
        code.append("package ").append(packageName).append(".controller;\n\n");
        code.append("import org.junit.jupiter.api.Test;\n");
        code.append("import org.springframework.beans.factory.annotation.Autowired;\n");
        code.append("import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;\n");
        code.append("import org.springframework.boot.test.mock.mockito.MockBean;\n");
        code.append("import org.springframework.test.web.servlet.MockMvc;\n\n");
        code.append("@WebMvcTest(").append(controllerName).append(".class)\n");
        code.append("public class ").append(className).append(" {\n\n");
        code.append("    @Autowired\n");
        code.append("    private MockMvc mockMvc;\n\n");
        code.append("    @MockBean\n");
        code.append("    private ").append(packageName).append(".service.").append(entityName).append("Service service;\n\n");
        code.append("    @Test\n");
        code.append("    public void testGetAll() throws Exception {\n");
        code.append("        // TODO: Implement test\n");
        code.append("    }\n\n");
        code.append("    @Test\n");
        code.append("    public void testGetById() throws Exception {\n");
        code.append("        // TODO: Implement test\n");
        code.append("    }\n\n");
        code.append("    @Test\n");
        code.append("    public void testCreate() throws Exception {\n");
        code.append("        // TODO: Implement test\n");
        code.append("    }\n\n");
        code.append("    @Test\n");
        code.append("    public void testUpdate() throws Exception {\n");
        code.append("        // TODO: Implement test\n");
        code.append("    }\n\n");
        code.append("    @Test\n");
        code.append("    public void testDelete() throws Exception {\n");
        code.append("        // TODO: Implement test\n");
        code.append("    }\n");
        code.append("}\n");
        
        writeFile("src/test/java/" + packageName.replace(".", "/") + "/controller/" + className + ".java", code.toString());
    }

    private void writeFile(String relativePath, String content) throws IOException {
        Path filePath = outputBasePath.resolve(relativePath);
        Files.createDirectories(filePath.getParent());
        Files.writeString(filePath, content);
    }

    private String capitalize(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }
}

