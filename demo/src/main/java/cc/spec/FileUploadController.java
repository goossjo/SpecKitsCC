package cc.spec;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.ui.Model;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

@Controller
public class FileUploadController {
    private static final String UPLOAD_DIR = System.getProperty("user.dir") + "/uploads";
    private static final String GENERATED_DIR = System.getProperty("user.dir") + "/uploads/generated";
    private static final String REFERENCE_SPEC_PATH = "src/main/java/cc/spec/specification_java_file.spec.md";

    @GetMapping("/")
    public String index() {
        return "upload";
    }

    @PostMapping("/upload")
    public String handleFileUpload(@RequestParam(required = false) MultipartFile openapi,
                                   @RequestParam(required = false) MultipartFile graphql,
                                   @RequestParam(required = false) MultipartFile domain,
                                   @RequestParam(required = false) MultipartFile testspec,
                                   @RequestParam(required = false) MultipartFile dependencies,
                                   @RequestParam(required = false) MultipartFile outputprefs,
                                   @RequestParam(required = false) String generationMode,
                                   RedirectAttributes redirectAttributes,
                                   Model model) {
        StringBuilder result = new StringBuilder();
        StringBuilder logs = new StringBuilder();
        Path uploadPath = Path.of(UPLOAD_DIR);
        Map<String, Path> uploadedFiles = new HashMap<>();
        
        try {
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }
            
            // Upload required files and track their paths
            MultipartFile[] requiredFiles = {openapi, graphql, domain, outputprefs};
            String[] requiredKeys = {"openapi", "graphql", "domain", "outputprefs"};
            String[] requiredNames = {"OpenAPI", "GraphQL", "Domain Model", "Output Preferences"};
            
            // Upload optional files
            MultipartFile[] optionalFiles = {testspec, dependencies};
            String[] optionalKeys = {"testspec", "dependencies"};
            String[] optionalNames = {"Test Spec", "Dependencies"};
            
            // Process required files
            for (int i = 0; i < requiredFiles.length; i++) {
                MultipartFile file = requiredFiles[i];
                if (file != null && !file.isEmpty()) {
                    Path filePath = uploadPath.resolve(file.getOriginalFilename());
                    file.transferTo(filePath.toFile());
                    uploadedFiles.put(requiredKeys[i], filePath);
                    result.append("Uploaded: ").append(file.getOriginalFilename()).append("<br>");
                    logs.append("[INFO] Uploaded ").append(requiredNames[i]).append(": ").append(file.getOriginalFilename()).append("\n");
                } else {
                    logs.append("[WARN] No file uploaded for ").append(requiredNames[i]).append("\n");
                }
            }
            
            // Process optional files
            for (int i = 0; i < optionalFiles.length; i++) {
                MultipartFile file = optionalFiles[i];
                if (file != null && !file.isEmpty()) {
                    Path filePath = uploadPath.resolve(file.getOriginalFilename());
                    file.transferTo(filePath.toFile());
                    uploadedFiles.put(optionalKeys[i], filePath);
                    result.append("Uploaded: ").append(file.getOriginalFilename()).append("<br>");
                    logs.append("[INFO] Uploaded ").append(optionalNames[i]).append(": ").append(file.getOriginalFilename()).append("\n");
                } else {
                    logs.append("[INFO] Optional file ").append(optionalNames[i]).append(" not provided (optional)\n");
                }
            }
            
            // Validate required files are present
            boolean hasAllRequired = uploadedFiles.containsKey("openapi") && 
                                    uploadedFiles.containsKey("graphql") && 
                                    uploadedFiles.containsKey("domain") &&
                                    uploadedFiles.containsKey("outputprefs");
            
            if (hasAllRequired) {
                logs.append("[INFO] Starting project generation...\n");
                logs.append("[INFO] Parsing specifications...\n");
                // Create output directory with timestamp and mode
                String timestamp = String.valueOf(System.currentTimeMillis());
                Path outputDir;
                if (generationMode != null && generationMode.equals("ai")) {
                    outputDir = Path.of(GENERATED_DIR, "ai", "project-" + timestamp);
                } else {
                    outputDir = Path.of(GENERATED_DIR, "classic", "project-" + timestamp);
                }
                Files.createDirectories(outputDir);
                logs.append("[INFO] Output directory: ").append(outputDir.toString()).append("\n");
                if (generationMode != null && generationMode.equals("ai")) {
                    logs.append("[INFO] Using AI Agent for code generation.\n");
                    logs.append("[INFO] Sending spec files to OpenAI API...\n");
                    // Only use the AI agent for generation
                    SpecToCodeAgent agent = new SpecToCodeAgent(REFERENCE_SPEC_PATH);
                    SpecToCodeAgent.GenerationResult generationResult = agent.generateProject(uploadedFiles, outputDir, true);
                    boolean aiUsed = false;
                    for (String gap : generationResult.getGaps()) {
                        if (gap != null && gap.contains("OpenAI API call")) {
                            aiUsed = true;
                            break;
                        }
                    }
                    if (Files.exists(outputDir.resolve("openai_response.json"))) {
                        aiUsed = true;
                    }
                    if (aiUsed) {
                        logs.append("[AI] AI-powered code generation was used. See openai_response.json for details.\n");
                        result.append("<br><strong>AI-powered code generation was used.</strong><br>");
                    }
                    logs.append("[SUCCESS] Project generation complete!\n");
                    logs.append("[INFO] Project location: ").append(outputDir.toAbsolutePath()).append("\n");
                    result.append("<br><strong>Project generated successfully!</strong><br>");
                    result.append("Location: ").append(outputDir.toAbsolutePath()).append("<br>");
                } else {
                    logs.append("[INFO] Using classic Code Generator (no AI).\n");
                    // Only use the classic code generator for generation
                    SpecToCodeAgent agent = new SpecToCodeAgent(REFERENCE_SPEC_PATH);
                    SpecToCodeAgent.GenerationResult generationResult = agent.generateProject(uploadedFiles, outputDir, false);
                    logs.append("[SUCCESS] Project generated using classic code generator!\n");
                    logs.append("[INFO] Project location: ").append(outputDir.toAbsolutePath()).append("\n");
                    result.append("<br><strong>Project generated using classic code generator!</strong><br>");
                    result.append("Location: ").append(outputDir.toAbsolutePath()).append("<br>");
                }
            } else {
                StringBuilder missingFiles = new StringBuilder();
                if (!uploadedFiles.containsKey("openapi")) missingFiles.append("OpenAPI, ");
                if (!uploadedFiles.containsKey("graphql")) missingFiles.append("GraphQL, ");
                if (!uploadedFiles.containsKey("domain")) missingFiles.append("Domain Model, ");
                if (!uploadedFiles.containsKey("outputprefs")) missingFiles.append("Output Preferences, ");
                
                if (missingFiles.length() > 0) {
                    missingFiles.setLength(missingFiles.length() - 2); // Remove trailing ", "
                }
                
                logs.append("[ERROR] Missing required specification files: ").append(missingFiles.toString()).append("\n");
                result.append("<br><strong>Missing required files:</strong> ").append(missingFiles.toString()).append("<br>");
                result.append("Please upload all required files: OpenAPI, GraphQL, Domain Model, and Output Preferences.");
            }
            
            model.addAttribute("message", result.length() > 0 ? result.toString() : "No files uploaded.");
            model.addAttribute("logs", logs.toString().replace("\n", "<br>"));
        } catch (Exception e) {
            String errorMsg = "Failed to process files: " + e.getMessage();
            logs.append("[ERROR] ").append(errorMsg).append("\n");
            if (e.getCause() != null) {
                logs.append("[ERROR] Cause: ").append(e.getCause().getMessage()).append("\n");
            }
            model.addAttribute("message", errorMsg);
            model.addAttribute("logs", logs.toString().replace("\n", "<br>"));
            e.printStackTrace();
        }
        return "upload";
    }
}
