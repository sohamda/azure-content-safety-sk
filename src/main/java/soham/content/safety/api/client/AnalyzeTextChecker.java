package soham.content.safety.api.client;

import com.azure.ai.contentsafety.ContentSafetyClient;
import com.azure.ai.contentsafety.ContentSafetyClientBuilder;
import com.azure.ai.contentsafety.models.AnalyzeTextOptions;
import com.azure.ai.contentsafety.models.AnalyzeTextResult;
import com.azure.ai.contentsafety.models.TextCategoriesAnalysis;
import com.azure.core.credential.KeyCredential;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

import static soham.content.safety.util.PropertyReader.getProperty;

public class AnalyzeTextChecker {

    private static final Logger log = LoggerFactory.getLogger(AnalyzeTextChecker.class);

    public static String CONTENT_SAFETY_ENDPOINT = "client.azure.content.safety.endpoint";
    public static String CONTENT_SAFETY_KEY = "client.azure.content.safety.key";

    private ContentSafetyClient contentSafetyClient;
    public AnalyzeTextChecker() throws IOException {

        contentSafetyClient = getContentSafetyClient();
    }

    private ContentSafetyClient getContentSafetyClient() throws IOException {
        String endpoint = getProperty(CONTENT_SAFETY_ENDPOINT);
        String key = getProperty(CONTENT_SAFETY_KEY);

        // Create a Content Safety client
        return new ContentSafetyClientBuilder()
                .credential(new KeyCredential(key))
                .endpoint(endpoint).buildClient();
    }

    public boolean isPromptSafe(String prompt) {

        AnalyzeTextResult analyzeTextResult = contentSafetyClient.analyzeText(new AnalyzeTextOptions(prompt));
        boolean isSafe = Boolean.TRUE;

        for(TextCategoriesAnalysis textCategoriesAnalysis : analyzeTextResult.getCategoriesAnalysis()) {
            log.debug("Category: {}, Severity: {}", textCategoriesAnalysis.getCategory(), textCategoriesAnalysis.getSeverity());
            if (textCategoriesAnalysis.getSeverity() > 1) {
                isSafe = Boolean.FALSE;
            }
        }
        return isSafe;
    }
}
