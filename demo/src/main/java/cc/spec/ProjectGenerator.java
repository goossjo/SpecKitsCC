package cc.spec;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 * Generates the project structure and files
 */
public class ProjectGenerator {
    private final Path outputBasePath;
    private final String packageName;
    private final String projectName;

    public ProjectGenerator(Path outputBasePath, String projectName, String packageName) {
        this.outputBasePath = outputBasePath;
        this.projectName = projectName;
        this.packageName = packageName;
    }

    /**
     * Creates the complete Maven project structure
     */
    public void createProjectStructure() throws IOException {
        // Create main directories
        createDirectory("src/main/java/" + packageName.replace(".", "/"));
        createDirectory("src/main/java/" + packageName.replace(".", "/") + "/controller");
        createDirectory("src/main/java/" + packageName.replace(".", "/") + "/service");
        createDirectory("src/main/java/" + packageName.replace(".", "/") + "/repository");
        createDirectory("src/main/java/" + packageName.replace(".", "/") + "/entity");
        createDirectory("src/main/java/" + packageName.replace(".", "/") + "/dto");
        createDirectory("src/main/resources");
        createDirectory("src/test/java/" + packageName.replace(".", "/"));
        createDirectory("src/test/java/" + packageName.replace(".", "/") + "/controller");
        createDirectory("src/test/java/" + packageName.replace(".", "/") + "/service");
    }

    /**
     * Generates pom.xml for the project
     */
    public void generatePomXml() throws IOException {
        String pomContent = String.format(""" 
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <groupId>%s</groupId>
    <artifactId>%s</artifactId>
    <version>1.0.0</version>
    <packaging>jar</packaging>

    <name>%s</name>

    <properties>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
        <maven.compiler.source>17</maven.compiler.source>
        <maven.compiler.target>17</maven.compiler.target>
        <spring.boot.version>3.2.5</spring.boot.version>
    </properties>

    <dependencies>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
            <version>${spring.boot.version}</version>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-jpa</artifactId>
            <version>${spring.boot.version}</version>
        </dependency>
        <dependency>
            <groupId>com.h2database</groupId>
            <artifactId>h2</artifactId>
            <version>2.2.224</version>
            <scope>runtime</scope>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <version>${spring.boot.version}</version>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
                <version>${spring.boot.version}</version>
            </plugin>
        </plugins>
    </build>
</project>
""", packageName, projectName.toLowerCase(), projectName);
        
        writeFile("pom.xml", pomContent);
    }

    /**
     * Generates application.properties
     */
    public void generateApplicationProperties() throws IOException {
        String propsContent = """
# Spring Boot Application Configuration
spring.application.name=%s

# Database Configuration (H2 in-memory for development)
spring.datasource.url=jdbc:h2:mem:testdb
spring.datasource.driverClassName=org.h2.Driver
spring.datasource.username=sa
spring.datasource.password=

# JPA Configuration
spring.jpa.database-platform=org.hibernate.dialect.H2Dialect
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true

# Server Configuration
server.port=8080
""".formatted(projectName);
        
        writeFile("src/main/resources/application.properties", propsContent);
    }

    /**
     * Generates the main application class
     */
    public void generateApplicationClass() throws IOException {
        String className = projectName + "Application";
        String content = String.format("""
package %s;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class %s {
    public static void main(String[] args) {
        SpringApplication.run(%s.class, args);
    }
}
""", packageName, className, className);
        
        writeFile("src/main/java/" + packageName.replace(".", "/") + "/" + className + ".java", content);
    }

    /**
     * Generates README.md
     */
    public void generateReadme(List<String> entities, List<String> gaps) throws IOException {
        StringBuilder readme = new StringBuilder();
        readme.append("# ").append(projectName).append("\n\n");
        readme.append("## Overview\n");
        readme.append("This project was automatically generated from specification files using SpecToCodeAgent.\n\n");
        
        readme.append("## Project Structure\n");
        readme.append("- **Entities**: Domain models representing database tables\n");
        readme.append("- **DTOs**: Data Transfer Objects for API requests/responses\n");
        readme.append("- **Repositories**: Data access layer interfaces\n");
        readme.append("- **Services**: Business logic layer\n");
        readme.append("- **Controllers**: REST API endpoints\n\n");
        
        if (!entities.isEmpty()) {
            readme.append("## Generated Entities\n");
            for (String entity : entities) {
                readme.append("- ").append(entity).append("\n");
            }
            readme.append("\n");
        }
        
        readme.append("## Building and Running\n\n");
        readme.append("### Prerequisites\n");
        readme.append("- Java 17 or higher\n");
        readme.append("- Maven 3.6+\n\n");
        readme.append("### Build\n");
        readme.append("```bash\n");
        readme.append("mvn clean install\n");
        readme.append("```\n\n");
        readme.append("### Run\n");
        readme.append("```bash\n");
        readme.append("mvn spring-boot:run\n");
        readme.append("```\n\n");
        
        if (!gaps.isEmpty()) {
            readme.append("## GAP Report\n");
            readme.append("The following items were identified as missing or ambiguous in the specifications:\n\n");
            for (String gap : gaps) {
                readme.append("- ").append(gap).append("\n");
            }
        }
        
        writeFile("README.md", readme.toString());
    }

    private void createDirectory(String relativePath) throws IOException {
        Path dirPath = outputBasePath.resolve(relativePath);
        Files.createDirectories(dirPath);
    }

    private void writeFile(String relativePath, String content) throws IOException {
        Path filePath = outputBasePath.resolve(relativePath);
        Files.createDirectories(filePath.getParent());
        Files.writeString(filePath, content);
    }
}

