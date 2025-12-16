package cc.spec;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import cc.spec.GapReportGenerator;
import cc.spec.SpecParser;
import cc.spec.SpecParser.EndpointInfo;
import cc.spec.SpecParser.EntityInfo;
import cc.spec.CodeGenerator;

/**
 * SpecToCodeAgent
 *
 * This agent reads formal specification files (OpenAPI, GraphQL, domain models, etc.) 
 * and generates code artifacts according to the specification.
 *
 * Specification context: see specification_java_file.spec.md for full details.
 */
public class SpecToCodeAgent {
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
     * @param uploadedFiles Map of file types to their paths
     * @param outputDirectory Where to generate the project
     * @param useAI Whether to use AI-powered code generation
     */
    public GenerationResult generateProject(Map<String, Path> uploadedFiles, Path outputDirectory, boolean useAI) throws IOException {
        // Parse uploaded files
        Map<String, Object> openAPISpec = null;
        String graphQLSchema = null;
        Map<String, Object> domainModel = null;
        Map<String, Object> metadata = null;
        Map<String, Object> outputPreferences = null;

        // If AI is enabled and any spec file is present, call OpenAIClient for code generation
        if (useAI) {
            try {
                OpenAIClient openAIClient = new OpenAIClient();
                StringBuilder promptBuilder = new StringBuilder();
                promptBuilder.append("Generate a Java Spring Boot project using the following specification files. Provide only the main code files as a JSON object with file paths as keys and file contents as values.\n");
                promptBuilder.append("Include all necessary Spring Boot components: entities, DTOs, repositories, services, controllers, and tests.\n\n");
                
                System.out.println("[AI] Preparing to send spec files to OpenAI agent...");
                int fileCount = 0;
                for (Map.Entry<String, Path> entry : uploadedFiles.entrySet()) {
                    String key = entry.getKey();
                    Path path = entry.getValue();
                    if (path != null && Files.exists(path)) {
                        try {
                            String content = Files.readString(path);
                            promptBuilder.append("\n--- ").append(key.toUpperCase()).append(" SPECIFICATION FILE ---\n");
                            promptBuilder.append("File type: ").append(key).append("\n");
                            promptBuilder.append("File name: ").append(path.getFileName()).append("\n");
                            promptBuilder.append("Content:\n").append(content).append("\n");
                            promptBuilder.append("--- END ").append(key.toUpperCase()).append(" FILE ---\n");
                            fileCount++;
                            System.out.println("[AI] ✓ Added " + key + " file: " + path.getFileName() + " (" + content.length() + " chars)");
                        } catch (Exception e) {
                            System.err.println("[AI] ✗ Failed to read " + key + " file: " + e.getMessage());
                            gapReportGenerator.addGap("Failed to read " + key + " file for AI: " + e.getMessage());
                        }
                    }
                }
                
                if (fileCount == 0) {
                    throw new IOException("No spec files were successfully read to send to OpenAI");
                }
                
                System.out.println("[AI] Sending " + fileCount + " spec file(s) to OpenAI API...");
                String prompt = promptBuilder.toString();
                System.out.println("[AI] Total prompt size: " + prompt.length() + " characters");
                String aiResponse = openAIClient.chatCompletion(prompt);
                System.out.println("[AI] ✓ Received response from OpenAI (" + aiResponse.length() + " chars)");
                // Parse the AI response as JSON and write files
                Files.createDirectories(outputDirectory);
                Path aiOut = outputDirectory.resolve("openai_response.json");
                Files.writeString(aiOut, aiResponse);
                System.out.println("[AI] ✓ Saved OpenAI response to: " + aiOut);
                
                // Try to parse as JSON and write files
                try {
                    com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                    Map<String, String> fileMap = mapper.readValue(aiResponse, Map.class);
                    System.out.println("[AI] ✓ Parsed response as JSON, found " + fileMap.size() + " files to generate");
                    int generatedCount = 0;
                    for (Map.Entry<String, String> fileEntry : fileMap.entrySet()) {
                        Path filePath = outputDirectory.resolve(fileEntry.getKey());
                        Files.createDirectories(filePath.getParent());
                        Files.writeString(filePath, fileEntry.getValue());
                        generatedCount++;
                        System.out.println("[AI] ✓ Generated: " + fileEntry.getKey());
                    }
                    System.out.println("[AI] ✓ Successfully generated " + generatedCount + " files from AI response");
                } catch (Exception jsonEx) {
                    System.err.println("[AI] ✗ Failed to parse OpenAI response as JSON: " + jsonEx.getMessage());
                    gapReportGenerator.addGap("Failed to parse OpenAI response as JSON: " + jsonEx.getMessage());
                }
            } catch (Exception e) {
                System.err.println("[AI] ✗ OpenAI API call failed: " + e.getMessage());
                e.printStackTrace();
                gapReportGenerator.addGap("OpenAI API call failed: " + e.getMessage());
            }
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
