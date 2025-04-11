package soham.content.safety.api.client;

import com.apisdk.models.ShieldPromptOptions;
import com.apisdk.models.ShieldPromptResult;
import com.apisdk.textshieldprompt.TextShieldPromptRequestBuilder;
import com.microsoft.kiota.authentication.AnonymousAuthenticationProvider;
import com.microsoft.kiota.bundle.DefaultRequestAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Collections;

import static soham.content.safety.util.PropertyReader.getProperty;

public class PromptShieldChecker {

    private static final Logger log = LoggerFactory.getLogger(PromptShieldChecker.class);

    public static String CONTENT_SAFETY_ENDPOINT = "client.azure.content.safety.endpoint";
    public static String CONTENT_SAFETY_KEY = "client.azure.content.safety.key";

    public boolean isPromptSafe(String prompt) throws IOException {

        ShieldPromptOptions shieldPromptOptions = new ShieldPromptOptions();
        shieldPromptOptions.setUserPrompt(prompt);

        final String key = getProperty(CONTENT_SAFETY_KEY);
        final String endpoint = getProperty(CONTENT_SAFETY_ENDPOINT);

        final AnonymousAuthenticationProvider authProvider =
                new AnonymousAuthenticationProvider();
        final DefaultRequestAdapter adapter = new DefaultRequestAdapter(authProvider);

        TextShieldPromptRequestBuilder textShieldPromptRequestBuilder = new TextShieldPromptRequestBuilder(
                endpoint + "/text:shieldPrompt?api-version=2024-09-15-preview", adapter);

        ShieldPromptResult post = textShieldPromptRequestBuilder.post(shieldPromptOptions,
                requestConfiguration -> {
                    requestConfiguration.headers.put("Ocp-Apim-Subscription-Key", Collections.singleton(key));
                }
        );
        Boolean attackDetected = post.getUserPromptAnalysis().getAttackDetected();
        log.debug("Is prompt shield checked: {}", attackDetected);
        return attackDetected;
    }
}
