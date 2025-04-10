package soham.content.safety;

import com.azure.ai.openai.OpenAIAsyncClient;
import com.azure.ai.openai.OpenAIClientBuilder;
import com.azure.core.credential.AzureKeyCredential;
import com.azure.core.credential.KeyCredential;
import com.microsoft.semantickernel.Kernel;
import com.microsoft.semantickernel.aiservices.openai.chatcompletion.OpenAIChatCompletion;
import com.microsoft.semantickernel.orchestration.FunctionResult;
import com.microsoft.semantickernel.orchestration.PromptExecutionSettings;
import com.microsoft.semantickernel.semanticfunctions.KernelFunction;
import com.microsoft.semantickernel.semanticfunctions.KernelFunctionArguments;
import com.microsoft.semantickernel.semanticfunctions.KernelFunctionFromPrompt;
import com.microsoft.semantickernel.services.chatcompletion.ChatCompletionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

import static soham.content.safety.util.PropertyReader.getProperty;

public class InvokeLLM {
    private static final Logger log = LoggerFactory.getLogger(InvokeLLM.class);

    public static String ENDPOINT = "client.azureopenai.endpoint";
    public static String API_KEY = "client.azureopenai.key";
    public static String MODEL_NAME = "client.azureopenai.deploymentname";

    private ContentSafetyChecker contentSafetyChecker;
    Kernel kernel;
    KernelFunction<Object> executePromptFunction;

    public InvokeLLM() throws IOException {
        contentSafetyChecker = new ContentSafetyChecker();
        log.debug("== Instantiates the Kernel ==");
        kernel = chatCompletionKernel();
        log.debug("== Define inline function ==");
        executePromptFunction = createKernelFunction();
    }

    public String runPrompt(String prompt, boolean validateInput, boolean validateOutput) throws IOException {
        String promptResponse;
        if(validateInput && !contentSafetyChecker.isContentSafe(prompt)) {
            promptResponse = "Input is not safe";
        } else {
            promptResponse = executeViaKernel(prompt);
            if (validateOutput && !contentSafetyChecker.isContentSafe(promptResponse)) {
                promptResponse = "Output is not safe";
            }
        }

        log.debug("Execution results: {}", promptResponse);
        return promptResponse;
    }

    private String executeViaKernel(String prompt) {
        FunctionResult<Object> result = kernel
                .invokeAsync(executePromptFunction)
                .withArguments(
                        KernelFunctionArguments.builder()
                                .withInput(prompt)
                                .build())
                .block();
        return (String) result.getResult();
    }
    private KernelFunction<Object> createKernelFunction() {
        String promptTemplate = """
            {{$input}}
            
            """;
        log.debug("== Configure the prompt execution settings ==");
        KernelFunction<Object> executeFunction = KernelFunctionFromPrompt.builder()
                .withTemplate(promptTemplate)
                .withDefaultExecutionSettings(
                        PromptExecutionSettings.builder()
                                .withTemperature(0.4)
                                .withTopP(1)
                                .withMaxTokens(1000)
                                .build())
                .build();
        return executeFunction;
    }

    private Kernel chatCompletionKernel() throws IOException {
        String modelName = getProperty(MODEL_NAME);
        OpenAIAsyncClient client = openAIAsyncClient();
        ChatCompletionService chatCompletion = OpenAIChatCompletion.builder()
                .withOpenAIAsyncClient(client)
                .withModelId(modelName)
                .build();
        return Kernel.builder()
                    .withAIService(ChatCompletionService.class, chatCompletion)
                    .build();
    }

    private OpenAIAsyncClient openAIAsyncClient() throws IOException {
        String endpoint = getProperty(ENDPOINT);
        String apiKey = getProperty(API_KEY);

        if(endpoint != null && !endpoint.isEmpty()) {
            return new OpenAIClientBuilder()
                    .endpoint(endpoint)
                    .credential(new AzureKeyCredential(apiKey))
                    .buildAsyncClient();
        }
        return new OpenAIClientBuilder()
                .credential(new KeyCredential(apiKey))
                .buildAsyncClient();
    }
}
