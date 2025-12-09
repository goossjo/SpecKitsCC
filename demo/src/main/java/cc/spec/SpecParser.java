package cc.spec;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * Parses various specification file formats (OpenAPI, GraphQL, Domain Models, etc.)
 */
public class SpecParser {
    private final ObjectMapper yamlMapper;
    private final ObjectMapper jsonMapper;

    public SpecParser() {
        this.yamlMapper = new ObjectMapper(new YAMLFactory());
        this.jsonMapper = new ObjectMapper();
    }

    /**
     * Parses an OpenAPI/Swagger specification file
     */
    public Map<String, Object> parseOpenAPI(Path filePath) throws IOException {
        String content = Files.readString(filePath);
        if (filePath.toString().endsWith(".yaml") || filePath.toString().endsWith(".yml")) {
            return yamlMapper.readValue(content, Map.class);
        } else {
            return jsonMapper.readValue(content, Map.class);
        }
    }

    /**
     * Parses a GraphQL schema file
     */
    public String parseGraphQL(Path filePath) throws IOException {
        return Files.readString(filePath);
    }

    /**
     * Parses a domain model YAML file
     */
    public Map<String, Object> parseDomainModel(Path filePath) throws IOException {
        String content = Files.readString(filePath);
        return yamlMapper.readValue(content, Map.class);
    }

    /**
     * Parses a metadata YAML file
     */
    public Map<String, Object> parseMetadata(Path filePath) throws IOException {
        String content = Files.readString(filePath);
        return yamlMapper.readValue(content, Map.class);
    }

    /**
     * Parses output preferences YAML file
     */
    public Map<String, Object> parseOutputPreferences(Path filePath) throws IOException {
        String content = Files.readString(filePath);
        if (content.trim().isEmpty()) {
            return new HashMap<>();
        }
        return yamlMapper.readValue(content, Map.class);
    }

    /**
     * Extracts entities from OpenAPI spec
     */
    @SuppressWarnings("unchecked")
    public List<EntityInfo> extractEntitiesFromOpenAPI(Map<String, Object> openAPISpec) {
        List<EntityInfo> entities = new ArrayList<>();
        Map<String, Object> components = (Map<String, Object>) openAPISpec.get("components");
        if (components != null) {
            Map<String, Object> schemas = (Map<String, Object>) components.get("schemas");
            if (schemas != null) {
                for (Map.Entry<String, Object> entry : schemas.entrySet()) {
                    String entityName = entry.getKey();
                    Map<String, Object> schema = (Map<String, Object>) entry.getValue();
                    EntityInfo entity = new EntityInfo(entityName);
                    
                    Map<String, Object> properties = (Map<String, Object>) schema.get("properties");
                    if (properties != null) {
                        for (Map.Entry<String, Object> propEntry : properties.entrySet()) {
                            String fieldName = propEntry.getKey();
                            Map<String, Object> fieldSchema = (Map<String, Object>) propEntry.getValue();
                            String fieldType = (String) fieldSchema.get("type");
                            entity.addField(fieldName, mapOpenAPITypeToJava(fieldType));
                        }
                    }
                    entities.add(entity);
                }
            }
        }
        return entities;
    }

    /**
     * Extracts API endpoints from OpenAPI spec
     */
    @SuppressWarnings("unchecked")
    public List<EndpointInfo> extractEndpointsFromOpenAPI(Map<String, Object> openAPISpec) {
        List<EndpointInfo> endpoints = new ArrayList<>();
        Map<String, Object> paths = (Map<String, Object>) openAPISpec.get("paths");
        if (paths != null) {
            for (Map.Entry<String, Object> pathEntry : paths.entrySet()) {
                String path = pathEntry.getKey();
                Map<String, Object> operations = (Map<String, Object>) pathEntry.getValue();
                
                for (Map.Entry<String, Object> opEntry : operations.entrySet()) {
                    String method = opEntry.getKey().toUpperCase();
                    Map<String, Object> operation = (Map<String, Object>) opEntry.getValue();
                    String summary = (String) operation.get("summary");
                    endpoints.add(new EndpointInfo(path, method, summary != null ? summary : ""));
                }
            }
        }
        return endpoints;
    }

    /**
     * Extracts entities from domain model
     */
    @SuppressWarnings("unchecked")
    public List<EntityInfo> extractEntitiesFromDomainModel(Map<String, Object> domainModel) {
        List<EntityInfo> entities = new ArrayList<>();
        Map<String, Object> entitiesMap = (Map<String, Object>) domainModel.get("entities");
        if (entitiesMap != null) {
            for (Map.Entry<String, Object> entry : entitiesMap.entrySet()) {
                String entityName = entry.getKey();
                Map<String, Object> entityData = (Map<String, Object>) entry.getValue();
                EntityInfo entity = new EntityInfo(entityName);
                
                Map<String, Object> fields = (Map<String, Object>) entityData.get("fields");
                if (fields != null) {
                    for (Map.Entry<String, Object> fieldEntry : fields.entrySet()) {
                        String fieldName = fieldEntry.getKey();
                        String fieldType = fieldEntry.getValue().toString();
                        entity.addField(fieldName, mapDomainTypeToJava(fieldType));
                    }
                }
                entities.add(entity);
            }
        }
        return entities;
    }

    private String mapOpenAPITypeToJava(String openAPIType) {
        if (openAPIType == null) return "String";
        return switch (openAPIType.toLowerCase()) {
            case "string" -> "String";
            case "integer" -> "Long";
            case "number" -> "Double";
            case "boolean" -> "Boolean";
            default -> "String";
        };
    }

    private String mapDomainTypeToJava(String domainType) {
        if (domainType == null) return "String";
        String lower = domainType.toLowerCase();
        if (lower.contains("int") || lower.contains("long")) return "Long";
        if (lower.contains("double") || lower.contains("float")) return "Double";
        if (lower.contains("bool")) return "Boolean";
        return "String";
    }

    /**
     * Simple data classes for entity and endpoint information
     */
    public static class EntityInfo {
        private final String name;
        private final Map<String, String> fields = new LinkedHashMap<>();

        public EntityInfo(String name) {
            this.name = name;
        }

        public void addField(String name, String type) {
            fields.put(name, type);
        }

        public String getName() { return name; }
        public Map<String, String> getFields() { return fields; }
    }

    public static class EndpointInfo {
        private final String path;
        private final String method;
        private final String summary;

        public EndpointInfo(String path, String method, String summary) {
            this.path = path;
            this.method = method;
            this.summary = summary;
        }

        public String getPath() { return path; }
        public String getMethod() { return method; }
        public String getSummary() { return summary; }
    }
}

