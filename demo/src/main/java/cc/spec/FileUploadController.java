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
import cc.spec.ai.SpecToCodeAgent;
import cc.spec.classic.CodeGenerator;

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
                                   @RequestParam(required = false) MultipartFile style,
                                   @RequestParam(required = false) MultipartFile metadata,
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
            
            // Upload files and track their paths
            MultipartFile[] files = {openapi, graphql, domain, testspec, style, metadata, dependencies, outputprefs};
            String[] keys = {"openapi", "graphql", "domain", "testspec", "style", "metadata", "dependencies", "outputprefs"};
            String[] names = {"OpenAPI", "GraphQL", "Domain Model", "Test Spec", "Style", "Metadata", "Dependencies", "Output Prefs"};
            
            for (int i = 0; i < files.length; i++) {
                MultipartFile file = files[i];
                if (file != null && !file.isEmpty()) {
                    Path filePath = uploadPath.resolve(file.getOriginalFilename());
                    file.transferTo(filePath.toFile());
                    uploadedFiles.put(keys[i], filePath);
                    result.append("Uploaded: ").append(file.getOriginalFilename()).append("<br>");
                    logs.append("[INFO] Uploaded ").append(names[i]).append(": ").append(file.getOriginalFilename()).append("\n");
                } else {
                    logs.append("[WARN] No file uploaded for ").append(names[i]).append("\n");
                }
            }
            
            // Check if we have at least one spec file to generate from
            boolean hasSpecFile = uploadedFiles.containsKey("openapi") || 
                                 uploadedFiles.containsKey("graphql") || 
                                 uploadedFiles.containsKey("domain");
            
            if (hasSpecFile) {
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
                logs.append("[WARN] No specification files provided. Upload OpenAPI, GraphQL, or Domain Model files to generate a project.\n");
                result.append("<br><strong>No specification files uploaded.</strong> Please upload at least one spec file (OpenAPI, GraphQL, or Domain Model) to generate a project.");
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
