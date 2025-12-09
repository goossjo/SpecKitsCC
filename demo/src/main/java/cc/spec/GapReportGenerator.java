package cc.spec;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * Generates GAP reports for missing or ambiguous specification elements
 */
public class GapReportGenerator {
    private final List<String> gaps = new ArrayList<>();

    public void addGap(String description) {
        gaps.add(description);
    }

    public void checkEntityCompleteness(SpecParser.EntityInfo entity) {
        if (entity.getFields().isEmpty()) {
            addGap("Entity '" + entity.getName() + "' has no fields defined");
        }
        
        boolean hasId = false;
        for (String fieldName : entity.getFields().keySet()) {
            if (fieldName.equalsIgnoreCase("id")) {
                hasId = true;
                break;
            }
        }
        if (!hasId) {
            addGap("Entity '" + entity.getName() + "' does not have an explicit 'id' field (auto-generated)");
        }
    }

    public void checkEndpointCompleteness(SpecParser.EndpointInfo endpoint) {
        if (endpoint.getSummary() == null || endpoint.getSummary().isEmpty()) {
            addGap("Endpoint " + endpoint.getMethod() + " " + endpoint.getPath() + " lacks a summary/description");
        }
    }

    public void checkMissingSpecFiles(boolean hasOpenAPI, boolean hasGraphQL, boolean hasDomainModel) {
        if (!hasOpenAPI && !hasGraphQL && !hasDomainModel) {
            addGap("No API specification files provided (OpenAPI, GraphQL, or Domain Model)");
        }
    }

    public void generateGapReport(Path outputPath) throws IOException {
        StringBuilder report = new StringBuilder();
        report.append("# GAP Report\n\n");
        report.append("Generated: ").append(new java.util.Date()).append("\n\n");
        
        if (gaps.isEmpty()) {
            report.append("## Status: No Gaps Found\n\n");
            report.append("All specification elements appear to be complete and unambiguous.\n");
        } else {
            report.append("## Status: ").append(gaps.size()).append(" Gap(s) Identified\n\n");
            report.append("The following items were identified as missing or ambiguous:\n\n");
            
            for (int i = 0; i < gaps.size(); i++) {
                report.append(i + 1).append(". ").append(gaps.get(i)).append("\n");
            }
        }
        
        Files.writeString(outputPath, report.toString());
    }

    public List<String> getGaps() {
        return new ArrayList<>(gaps);
    }

    public boolean hasGaps() {
        return !gaps.isEmpty();
    }
}

