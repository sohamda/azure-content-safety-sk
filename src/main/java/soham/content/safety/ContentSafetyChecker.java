package soham.content.safety;

import com.azure.ai.contentsafety.BlocklistClient;
import com.azure.ai.contentsafety.BlocklistClientBuilder;
import com.azure.ai.contentsafety.ContentSafetyClient;
import com.azure.ai.contentsafety.ContentSafetyClientBuilder;
import com.azure.ai.contentsafety.models.*;
import com.azure.core.credential.KeyCredential;
import com.azure.core.http.rest.RequestOptions;
import com.azure.core.http.rest.Response;
import com.azure.core.util.BinaryData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static soham.content.safety.util.PropertyReader.getProperty;

public class ContentSafetyChecker {

    private static final Logger log = LoggerFactory.getLogger(ContentSafetyChecker.class);

    public static String CONTENT_SAFETY_ENDPOINT = "client.azure.content.safety.endpoint";
    public static String CONTENT_SAFETY_KEY = "client.azure.content.safety.key";

    private ContentSafetyClient contentSafetyClient;
    private BlocklistClient blocklistClient;

    private List<String> blockedDrugs;

    public ContentSafetyChecker() throws IOException {
        contentSafetyClient = getContentSafetyClient();
        blocklistClient = getBlocklistClient();
        blockedDrugs = Arrays.asList("mdma", "cocaine", "opioids", "heroin", "meth", "crack");
    }

    private ContentSafetyClient getContentSafetyClient() throws IOException {
        String endpoint = getProperty(CONTENT_SAFETY_ENDPOINT);
        String key = getProperty(CONTENT_SAFETY_KEY);

        // Create a Content Safety client
        return new ContentSafetyClientBuilder()
                .credential(new KeyCredential(key))
                .endpoint(endpoint).buildClient();
    }

    private BlocklistClient getBlocklistClient() throws IOException {
        String endpoint = getProperty(CONTENT_SAFETY_ENDPOINT);
        String key = getProperty(CONTENT_SAFETY_KEY);

        // Create a Blocklist client
        return new BlocklistClientBuilder()
                .credential(new KeyCredential(key))
                .endpoint(endpoint).buildClient();
    }

    public boolean isContentSafe(String content) throws IOException {
        // Create a Content Safety client
        log.debug("== Content Safety Client == {}", content);

        AnalyzeTextOptions analyzeTextOptions = new AnalyzeTextOptions(content);
        analyzeTextOptions.setBlocklistNames(Arrays.asList(createUpdateBlocklist()));
        analyzeTextOptions.setHaltOnBlocklistHit(true);

        // Analyze text
        AnalyzeTextResult response = contentSafetyClient.analyzeText(analyzeTextOptions);

        boolean isSafe = true;
        for (TextCategoriesAnalysis result : response.getCategoriesAnalysis()) {
            log.debug(result.getCategory() + " severity: " + result.getSeverity());
            if(result.getSeverity() > 0) {
                log.debug("Content is not safe");
                isSafe = false;
                break;
            }
        }

        if (response.getBlocklistsMatch() != null) {
            log.debug("Blocklist match result:");
            for (TextBlocklistMatch matchResult : response.getBlocklistsMatch()) {
                log.debug("BlocklistName: " + matchResult.getBlocklistName() +
                        ", BlockItemId: " + matchResult.getBlocklistItemId() +
                        ", BlockItemText: " + matchResult.getBlocklistItemText());
            }
            isSafe = false;
        }

        return isSafe;
    }

    private String createUpdateBlocklist() throws IOException {
        String blocklistName = "ForbiddenItems";
        Map<String, String> description = new HashMap<>();
        description.put("description", "Drugs Blocklist");
        BinaryData resource = BinaryData.fromObject(description);
        RequestOptions requestOptions = new RequestOptions();
        Response<BinaryData> response =
                blocklistClient.createOrUpdateTextBlocklistWithResponse(blocklistName, resource, requestOptions);
        if (response.getStatusCode() == 201) {
            log.debug("Blocklist " + blocklistName + " created.");
        } else if (response.getStatusCode() == 200) {
            log.debug("Blocklist " + blocklistName + " updated.");
        }

        // Add blocklist items
        List<TextBlocklistItem> blockItems = blockedDrugs.stream()
                .map(s -> new TextBlocklistItem(s).setDescription("drugs"))
                .collect(Collectors.toList());
        AddOrUpdateTextBlocklistItemsResult addedBlockItems = blocklistClient.addOrUpdateBlocklistItems(blocklistName,
                new AddOrUpdateTextBlocklistItemsOptions(blockItems));
        log.debug("Blocklist items added to blocklist " + blocklistName + ":");
        addedBlockItems.getBlocklistItems().stream().forEach(
                    blocklistItem -> log.debug("BlockItemId: " + blocklistItem.getBlocklistItemId() +
                            ", Text: " + blocklistItem.getText() +
                            ", Description: " + blocklistItem.getDescription()));

        return blocklistName;
    }
}
