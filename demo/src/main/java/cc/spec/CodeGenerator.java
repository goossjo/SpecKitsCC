package cc.spec;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * Generates Java code artifacts (entities, DTOs, repositories, services, controllers)
 */
public class CodeGenerator {
    private final Path outputBasePath;
    private final String packageName;

    public CodeGenerator(Path outputBasePath, String packageName) {
        this.outputBasePath = outputBasePath;
        this.packageName = packageName;
    }

    /**
     * Generates an entity class
     */
    public void generateEntity(SpecParser.EntityInfo entityInfo) throws IOException {
        String className = entityInfo.getName();
        StringBuilder code = new StringBuilder();
        code.append("package ").append(packageName).append(".entity;\n\n");
        code.append("import jakarta.persistence.*;\n");
        code.append("import java.util.Objects;\n\n");
        code.append("@Entity\n");
        code.append("@Table(name = \"").append(className.toLowerCase()).append("s\")\n");
        code.append("public class ").append(className).append(" {\n\n");
        
        // Fields
        boolean hasId = false;
        for (Map.Entry<String, String> field : entityInfo.getFields().entrySet()) {
            String fieldName = field.getKey();
            String fieldType = field.getValue();
            
            if (fieldName.equalsIgnoreCase("id")) {
                code.append("    @Id\n");
                code.append("    @GeneratedValue(strategy = GenerationType.IDENTITY)\n");
                hasId = true;
            }
            code.append("    private ").append(fieldType).append(" ").append(fieldName).append(";\n\n");
        }
        
        if (!hasId) {
            code.insert(code.indexOf("public class"), 
                "    @Id\n    @GeneratedValue(strategy = GenerationType.IDENTITY)\n    private Long id;\n\n");
        }
        
        // Default constructor
        code.append("    public ").append(className).append("() {\n");
        code.append("    }\n\n");
        
        // Getters and setters
        if (!hasId) {
            code.append("    public Long getId() {\n");
            code.append("        return id;\n");
            code.append("    }\n\n");
            code.append("    public void setId(Long id) {\n");
            code.append("        this.id = id;\n");
            code.append("    }\n\n");
        }
        
        for (Map.Entry<String, String> field : entityInfo.getFields().entrySet()) {
            String fieldName = field.getKey();
            String fieldType = field.getValue();
            String capitalized = capitalize(fieldName);
            
            code.append("    public ").append(fieldType).append(" get").append(capitalized).append("() {\n");
            code.append("        return ").append(fieldName).append(";\n");
            code.append("    }\n\n");
            code.append("    public void set").append(capitalized).append("(").append(fieldType).append(" ").append(fieldName).append(") {\n");
            code.append("        this.").append(fieldName).append(" = ").append(fieldName).append(";\n");
            code.append("    }\n\n");
        }
        
        code.append("}\n");
        
        writeFile("src/main/java/" + packageName.replace(".", "/") + "/entity/" + className + ".java", code.toString());
    }

    /**
     * Generates a DTO class
     */
    public void generateDTO(SpecParser.EntityInfo entityInfo) throws IOException {
        String className = entityInfo.getName() + "DTO";
        StringBuilder code = new StringBuilder();
        code.append("package ").append(packageName).append(".dto;\n\n");
        code.append("public class ").append(className).append(" {\n\n");
        
        // Fields
        for (Map.Entry<String, String> field : entityInfo.getFields().entrySet()) {
            String fieldName = field.getKey();
            String fieldType = field.getValue();
            code.append("    private ").append(fieldType).append(" ").append(fieldName).append(";\n");
        }
        code.append("\n");
        
        // Default constructor
        code.append("    public ").append(className).append("() {\n");
        code.append("    }\n\n");
        
        // Getters and setters
        for (Map.Entry<String, String> field : entityInfo.getFields().entrySet()) {
            String fieldName = field.getKey();
            String fieldType = field.getValue();
            String capitalized = capitalize(fieldName);
            
            code.append("    public ").append(fieldType).append(" get").append(capitalized).append("() {\n");
            code.append("        return ").append(fieldName).append(";\n");
            code.append("    }\n\n");
            code.append("    public void set").append(capitalized).append("(").append(fieldType).append(" ").append(fieldName).append(") {\n");
            code.append("        this.").append(fieldName).append(" = ").append(fieldName).append(";\n");
            code.append("    }\n\n");
        }
        
        code.append("}\n");
        
        writeFile("src/main/java/" + packageName.replace(".", "/") + "/dto/" + className + ".java", code.toString());
    }

    /**
     * Generates a repository interface
     */
    public void generateRepository(SpecParser.EntityInfo entityInfo) throws IOException {
        String className = entityInfo.getName() + "Repository";
        String entityName = entityInfo.getName();
        String code = String.format("""
package %s.repository;

import %s.entity.%s;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface %s extends JpaRepository<%s, Long> {
    // Add custom query methods here if needed
}
""", packageName, packageName, entityName, className, entityName);
        
        writeFile("src/main/java/" + packageName.replace(".", "/") + "/repository/" + className + ".java", code);
    }

    /**
     * Generates a service interface
     */
    public void generateServiceInterface(SpecParser.EntityInfo entityInfo) throws IOException {
        String className = entityInfo.getName() + "Service";
        String entityName = entityInfo.getName();
        String dtoName = entityName + "DTO";
        String code = String.format("""
package %s.service;

import %s.dto.%s;
import java.util.List;
import java.util.Optional;

public interface %s {
    List<%s> findAll();
    Optional<%s> findById(Long id);
    %s save(%s dto);
    void deleteById(Long id);
}
""", packageName, packageName, dtoName, className, dtoName, dtoName, dtoName, dtoName);
        
        writeFile("src/main/java/" + packageName.replace(".", "/") + "/service/" + className + ".java", code);
    }

    /**
     * Generates a service implementation
     */
    public void generateServiceImpl(SpecParser.EntityInfo entityInfo) throws IOException {
        String className = entityInfo.getName() + "ServiceImpl";
        String entityName = entityInfo.getName();
        String dtoName = entityName + "DTO";
        String serviceName = entityName + "Service";
        String repoName = entityName + "Repository";
        
        StringBuilder code = new StringBuilder();
        code.append("package ").append(packageName).append(".service;\n\n");
        code.append("import ").append(packageName).append(".dto.").append(dtoName).append(";\n");
        code.append("import ").append(packageName).append(".entity.").append(entityName).append(";\n");
        code.append("import ").append(packageName).append(".repository.").append(repoName).append(";\n");
        code.append("import org.springframework.beans.factory.annotation.Autowired;\n");
        code.append("import org.springframework.stereotype.Service;\n");
        code.append("import java.util.List;\n");
        code.append("import java.util.Optional;\n");
        code.append("import java.util.stream.Collectors;\n\n");
        code.append("@Service\n");
        code.append("public class ").append(className).append(" implements ").append(serviceName).append(" {\n\n");
        code.append("    @Autowired\n");
        code.append("    private ").append(repoName).append(" repository;\n\n");
        code.append("    @Override\n");
        code.append("    public List<").append(dtoName).append("> findAll() {\n");
        code.append("        return repository.findAll().stream()\n");
        code.append("                .map(this::toDTO)\n");
        code.append("                .collect(Collectors.toList());\n");
        code.append("    }\n\n");
        code.append("    @Override\n");
        code.append("    public Optional<").append(dtoName).append("> findById(Long id) {\n");
        code.append("        return repository.findById(id).map(this::toDTO);\n");
        code.append("    }\n\n");
        code.append("    @Override\n");
        code.append("    public ").append(dtoName).append(" save(").append(dtoName).append(" dto) {\n");
        code.append("        ").append(entityName).append(" entity = toEntity(dto);\n");
        code.append("        ").append(entityName).append(" saved = repository.save(entity);\n");
        code.append("        return toDTO(saved);\n");
        code.append("    }\n\n");
        code.append("    @Override\n");
        code.append("    public void deleteById(Long id) {\n");
        code.append("        repository.deleteById(id);\n");
        code.append("    }\n\n");
        code.append("    private ").append(dtoName).append(" toDTO(").append(entityName).append(" entity) {\n");
        code.append("        ").append(dtoName).append(" dto = new ").append(dtoName).append("();\n");
        for (Map.Entry<String, String> field : entityInfo.getFields().entrySet()) {
            String fieldName = field.getKey();
            String capitalized = capitalize(fieldName);
            code.append("        dto.set").append(capitalized).append("(entity.get").append(capitalized).append("());\n");
        }
        code.append("        return dto;\n");
        code.append("    }\n\n");
        code.append("    private ").append(entityName).append(" toEntity(").append(dtoName).append(" dto) {\n");
        code.append("        ").append(entityName).append(" entity = new ").append(entityName).append("();\n");
        for (Map.Entry<String, String> field : entityInfo.getFields().entrySet()) {
            String fieldName = field.getKey();
            String capitalized = capitalize(fieldName);
            code.append("        entity.set").append(capitalized).append("(dto.get").append(capitalized).append("());\n");
        }
        code.append("        return entity;\n");
        code.append("    }\n");
        code.append("}\n");
        
        writeFile("src/main/java/" + packageName.replace(".", "/") + "/service/" + className + ".java", code.toString());
    }

    /**
     * Generates a REST controller
     */
    public void generateController(SpecParser.EntityInfo entityInfo, List<SpecParser.EndpointInfo> endpoints) throws IOException {
        String className = entityInfo.getName() + "Controller";
        String entityName = entityInfo.getName();
        String dtoName = entityName + "DTO";
        String serviceName = entityName + "Service";
        
        StringBuilder code = new StringBuilder();
        code.append("package ").append(packageName).append(".controller;\n\n");
        code.append("import ").append(packageName).append(".dto.").append(dtoName).append(";\n");
        code.append("import ").append(packageName).append(".service.").append(serviceName).append(";\n");
        code.append("import org.springframework.beans.factory.annotation.Autowired;\n");
        code.append("import org.springframework.http.HttpStatus;\n");
        code.append("import org.springframework.http.ResponseEntity;\n");
        code.append("import org.springframework.web.bind.annotation.*;\n");
        code.append("import java.util.List;\n");
        code.append("import java.util.Optional;\n\n");
        code.append("@RestController\n");
        code.append("@RequestMapping(\"/api/").append(entityName.toLowerCase()).append("s\")\n");
        code.append("public class ").append(className).append(" {\n\n");
        code.append("    @Autowired\n");
        code.append("    private ").append(serviceName).append(" service;\n\n");
        
        // Generate endpoints based on OpenAPI spec or defaults
        boolean hasGetAll = false;
        boolean hasGetById = false;
        boolean hasPost = false;
        boolean hasDelete = false;
        
        for (SpecParser.EndpointInfo endpoint : endpoints) {
            if (endpoint.getPath().contains(entityName.toLowerCase())) {
                String method = endpoint.getMethod();
                if (method.equals("GET") && !hasGetAll) {
                    code.append("    @GetMapping\n");
                    code.append("    public ResponseEntity<List<").append(dtoName).append(">> getAll").append(entityName).append("s() {\n");
                    code.append("        List<").append(dtoName).append("> items = service.findAll();\n");
                    code.append("        return ResponseEntity.ok(items);\n");
                    code.append("    }\n\n");
                    hasGetAll = true;
                } else if (method.equals("POST") && !hasPost) {
                    code.append("    @PostMapping\n");
                    code.append("    public ResponseEntity<").append(dtoName).append("> create").append(entityName).append("(@RequestBody ").append(dtoName).append(" dto) {\n");
                    code.append("        ").append(dtoName).append(" created = service.save(dto);\n");
                    code.append("        return ResponseEntity.status(HttpStatus.CREATED).body(created);\n");
                    code.append("    }\n\n");
                    hasPost = true;
                }
            }
        }
        
        // Add default endpoints if not found in spec
        if (!hasGetAll) {
            code.append("    @GetMapping\n");
            code.append("    public ResponseEntity<List<").append(dtoName).append(">> getAll").append(entityName).append("s() {\n");
            code.append("        List<").append(dtoName).append("> items = service.findAll();\n");
            code.append("        return ResponseEntity.ok(items);\n");
            code.append("    }\n\n");
        }
        
        code.append("    @GetMapping(\"/{id}\")\n");
        code.append("    public ResponseEntity<").append(dtoName).append("> get").append(entityName).append("ById(@PathVariable Long id) {\n");
        code.append("        Optional<").append(dtoName).append("> item = service.findById(id);\n");
        code.append("        return item.map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());\n");
        code.append("    }\n\n");
        
        if (!hasPost) {
            code.append("    @PostMapping\n");
            code.append("    public ResponseEntity<").append(dtoName).append("> create").append(entityName).append("(@RequestBody ").append(dtoName).append(" dto) {\n");
            code.append("        ").append(dtoName).append(" created = service.save(dto);\n");
            code.append("        return ResponseEntity.status(HttpStatus.CREATED).body(created);\n");
            code.append("    }\n\n");
        }
        
        code.append("    @DeleteMapping(\"/{id}\")\n");
        code.append("    public ResponseEntity<Void> delete").append(entityName).append("(@PathVariable Long id) {\n");
        code.append("        service.deleteById(id);\n");
        code.append("        return ResponseEntity.noContent().build();\n");
        code.append("    }\n");
        code.append("}\n");
        
        writeFile("src/main/java/" + packageName.replace(".", "/") + "/controller/" + className + ".java", code.toString());
    }

    /**
     * Generates a basic test class
     */
    public void generateTest(SpecParser.EntityInfo entityInfo) throws IOException {
        String className = entityInfo.getName() + "ControllerTest";
        String entityName = entityInfo.getName();
        String dtoName = entityName + "DTO";
        String serviceName = entityName + "Service";
        
        String code = String.format("""
package %s.controller;

import %s.dto.%s;
import %s.service.%s;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import java.util.Arrays;
import java.util.Optional;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(%sController.class)
public class %s {
    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private %s service;

    @Test
    public void testGetAll%s() throws Exception {
        // TODO: Implement test logic
    }

    @Test
    public void testGet%sById() throws Exception {
        // TODO: Implement test logic
    }

    @Test
    public void testCreate%s() throws Exception {
        // TODO: Implement test logic
    }
}
""", packageName, packageName, dtoName, packageName, serviceName, entityName, className, serviceName, entityName, entityName, entityName);
        
        writeFile("src/test/java/" + packageName.replace(".", "/") + "/controller/" + className + ".java", code);
    }

    private String capitalize(String str) {
        if (str == null || str.isEmpty()) return str;
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }

    private void writeFile(String relativePath, String content) throws IOException {
        Path filePath = outputBasePath.resolve(relativePath);
        Files.createDirectories(filePath.getParent());
        Files.writeString(filePath, content);
    }
}

