package cc.spec.ai;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import cc.spec.GapReportGenerator;
import cc.spec.SpecParser.EndpointInfo;
import cc.spec.SpecParser.EntityInfo;
import cc.spec.classic.CodeGenerator;
import cc.spec.classic.ProjectGenerator;

public class SpecToCodeAgent {
    private final boolean useAI = true;
    private final String referenceSpecFilePath;
    private final SpecParser specParser;
    private final GapReportGenerator gapReportGenerator;

    /**
     * Constructs the agent with the path to the reference specification file.
     * @param referenceSpecFilePath Path to the reference specification file (e.g., specification_java_file.spec.md)
     */
    public SpecToCodeAgent(String referenceSpecFilePath) {
        this.referenceSpecFilePath = referenceSpecFilePath;
        this.specParser = new SpecParser();
        this.gapReportGenerator = new GapReportGenerator();
    }

    /**
     * Main method to generate a complete project from uploaded specification files
     */
    public GenerationResult generateProject(Map<String, Path> uploadedFiles, Path outputDirectory, boolean aiMode) throws IOException {
        // Parse uploaded files
        Map<String, Object> openAPISpec = null;
        String graphQLSchema = null;
        Map<String, Object> domainModel = null;
        Map<String, Object> metadata = null;
        Map<String, Object> outputPreferences = null;

        if (aiMode) {
            // Only use the AI agent for generation
            try {
                OpenAIClient openAIClient = new OpenAIClient();
                StringBuilder promptBuilder = new StringBuilder();
                promptBuilder.append("Generate a Java Spring Boot project using the following specification files. Provide only the main code files as a JSON object with file paths as keys and file contents as values.\n");
                for (Map.Entry<String, Path> entry : uploadedFiles.entrySet()) {
                    String key = entry.getKey();
                    Path path = entry.getValue();
                    if (path != null && Files.exists(path)) {
                        String content = Files.readString(path);
                        promptBuilder.append("\n--- " + key + " file ---\n");
                        promptBuilder.append(content).append("\n");
                    }
                }
                String prompt = promptBuilder.toString();
                String aiResponse = openAIClient.chatCompletion(prompt);
                // Parse the AI response as JSON and write files
                Files.createDirectories(outputDirectory);
                Path aiOut = outputDirectory.resolve("openai_response.json");
                Files.writeString(aiOut, aiResponse);
                // Try to parse as JSON and write files
                try {
                    com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                    Map<String, String> fileMap = mapper.readValue(aiResponse, Map.class);
                    for (Map.Entry<String, String> fileEntry : fileMap.entrySet()) {
                        Path filePath = outputDirectory.resolve(fileEntry.getKey());
                        Files.createDirectories(filePath.getParent());
                        Files.writeString(filePath, fileEntry.getValue());
                    }
                } catch (Exception jsonEx) {
                    gapReportGenerator.addGap("Failed to parse OpenAI response as JSON: " + jsonEx.getMessage());
                }
            } catch (Exception e) {
                gapReportGenerator.addGap("OpenAI API call failed: " + e.getMessage());
            }
            // Return minimal result for AI mode
            return new GenerationResult(outputDirectory, Collections.emptyList(), gapReportGenerator.getGaps());
        }

        boolean hasOpenAPI = uploadedFiles.containsKey("openapi") && uploadedFiles.get("openapi") != null;
        boolean hasGraphQL = uploadedFiles.containsKey("graphql") && uploadedFiles.get("graphql") != null;
        boolean hasDomainModel = uploadedFiles.containsKey("domain") && uploadedFiles.get("domain") != null;

        gapReportGenerator.checkMissingSpecFiles(hasOpenAPI, hasGraphQL, hasDomainModel);

        // Parse OpenAPI spec
        if (hasOpenAPI) {
            try {
                openAPISpec = specParser.parseOpenAPI(uploadedFiles.get("openapi"));
            } catch (Exception e) {
                gapReportGenerator.addGap("Failed to parse OpenAPI specification: " + e.getMessage());
            }
        }

        // Parse GraphQL schema
        if (hasGraphQL) {
            try {
                graphQLSchema = specParser.parseGraphQL(uploadedFiles.get("graphql"));
                gapReportGenerator.addGap("GraphQL schema parsing is not fully implemented - manual review required");
            } catch (Exception e) {
                gapReportGenerator.addGap("Failed to parse GraphQL schema: " + e.getMessage());
            }
        }

        // Parse domain model
        if (hasDomainModel) {
            try {
                domainModel = specParser.parseDomainModel(uploadedFiles.get("domain"));
            } catch (Exception e) {
                gapReportGenerator.addGap("Failed to parse domain model: " + e.getMessage());
            }
        }

        // Parse metadata
        if (uploadedFiles.containsKey("metadata") && uploadedFiles.get("metadata") != null) {
            try {
                metadata = specParser.parseMetadata(uploadedFiles.get("metadata"));
            } catch (Exception e) {
                // Metadata is optional, just log
            }
        }

        // Parse output preferences
        if (uploadedFiles.containsKey("outputprefs") && uploadedFiles.get("outputprefs") != null) {
            try {
                outputPreferences = specParser.parseOutputPreferences(uploadedFiles.get("outputprefs"));
            } catch (Exception e) {
                // Output preferences are optional
            }
        }

        // Extract project information
        String projectName = extractProjectName(openAPISpec, metadata);
        String packageName = extractPackageName(projectName, outputPreferences);

        // Extract entities
        List<SpecParser.EntityInfo> entities = new ArrayList<>();
        if (openAPISpec != null) {
            entities.addAll(specParser.extractEntitiesFromOpenAPI(openAPISpec));
        }
        if (domainModel != null) {
            entities.addAll(specParser.extractEntitiesFromDomainModel(domainModel));
        }

        // Extract endpoints
        List<SpecParser.EndpointInfo> endpoints = new ArrayList<>();
        if (openAPISpec != null) {
            endpoints.addAll(specParser.extractEndpointsFromOpenAPI(openAPISpec));
        }

        // Validate entities
        for (SpecParser.EntityInfo entity : entities) {
            gapReportGenerator.checkEntityCompleteness(entity);
        }

        // Validate endpoints
        for (SpecParser.EndpointInfo endpoint : endpoints) {
            gapReportGenerator.checkEndpointCompleteness(endpoint);
        }

        // Create project structure
        ProjectGenerator projectGenerator = new ProjectGenerator(outputDirectory, projectName, packageName);
        projectGenerator.createProjectStructure();
        projectGenerator.generatePomXml();
        projectGenerator.generateApplicationProperties();
        projectGenerator.generateApplicationClass();

        // Generate code artifacts
        CodeGenerator codeGenerator = new CodeGenerator(outputDirectory, packageName);
        List<String> entityNames = new ArrayList<>();
        
        for (SpecParser.EntityInfo entity : entities) {
            entityNames.add(entity.getName());
            codeGenerator.generateEntity(entity);
            codeGenerator.generateDTO(entity);
            codeGenerator.generateRepository(entity);
            codeGenerator.generateServiceInterface(entity);
            codeGenerator.generateServiceImpl(entity);
            
            // Generate controller with relevant endpoints
            List<SpecParser.EndpointInfo> entityEndpoints = endpoints.stream()
                .filter(e -> e.getPath().contains(entity.getName().toLowerCase()))
                .toList();
            codeGenerator.generateController(entity, entityEndpoints);
            codeGenerator.generateTest(entity);
        }

        // Generate README
        projectGenerator.generateReadme(entityNames, gapReportGenerator.getGaps());

        // Generate GAP report
        Path gapReportPath = outputDirectory.resolve("GAP_REPORT.md");
        gapReportGenerator.generateGapReport(gapReportPath);

        return new GenerationResult(outputDirectory, entityNames, gapReportGenerator.getGaps());
    }

    private String extractProjectName(Map<String, Object> openAPISpec, Map<String, Object> metadata) {
        if (metadata != null && metadata.containsKey("projectName")) {
            return metadata.get("projectName").toString();
        }
        if (openAPISpec != null) {
            @SuppressWarnings("unchecked")
            Map<String, Object> info = (Map<String, Object>) openAPISpec.get("info");
            if (info != null && info.containsKey("title")) {
                return info.get("title").toString().replaceAll("\\s+", "");
            }
        }
        return "GeneratedProject";
    }

    @SuppressWarnings("unchecked")
    private String extractPackageName(String projectName, Map<String, Object> outputPreferences) {
        if (outputPreferences != null && outputPreferences.containsKey("packageName")) {
            return outputPreferences.get("packageName").toString();
        }
        // Default package name based on project name
        return "com.example." + projectName.toLowerCase();
    }

    /**
     * Result class for generation operation
     */
    public static class GenerationResult {
        private final Path outputDirectory;
        private final List<String> generatedEntities;
        private final List<String> gaps;

        public GenerationResult(Path outputDirectory, List<String> generatedEntities, List<String> gaps) {
            this.outputDirectory = outputDirectory;
            this.generatedEntities = generatedEntities;
            this.gaps = gaps;
        }

        public Path getOutputDirectory() { return outputDirectory; }
        public List<String> getGeneratedEntities() { return generatedEntities; }
        public List<String> getGaps() { return gaps; }
    }

    /**
     * Main entry point for running the agent.
     */
    public static void main(String[] args) {
        String referenceSpecPath = "src/main/java/cc/spec/specification_java_file.spec.md";
        String incomingSpecPath = args.length > 0 ? args[0] : "";
        SpecToCodeAgent agent = new SpecToCodeAgent(referenceSpecPath);
        if (!incomingSpecPath.isEmpty()) {
            try {
                Map<String, Path> files = new HashMap<>();
                files.put("openapi", Path.of(incomingSpecPath));
                Path outputDir = Path.of("generated-project");
                GenerationResult result = agent.generateProject(files, outputDir, false);
                System.out.println("Project generated successfully at: " + result.getOutputDirectory());
            } catch (Exception e) {
                System.err.println("Error generating project: " + e.getMessage());
                e.printStackTrace();
            }
        } else {
            System.out.println("No incoming spec file provided. Only reference spec loaded.");
        }
    }
}
