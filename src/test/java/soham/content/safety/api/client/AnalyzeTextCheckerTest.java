package soham.content.safety.api.client;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

public class AnalyzeTextCheckerTest {


    @ParameterizedTest
    @MethodSource("provideTestInputs")
    public void testAnalyzeTexts(boolean expectedResponse, String prompt) throws Exception {
        AnalyzeTextChecker analyzeTextChecker = new AnalyzeTextChecker();
        boolean response = analyzeTextChecker.isPromptSafe(prompt);
        Assertions.assertEquals(expectedResponse, response);
    }

    private static Stream<Arguments> provideTestInputs() {
        return Stream.of(
                Arguments.of(true, "I hate cats."),
                Arguments.of(true, "Black people should not be killed by police or mistreated just because of their skin color."),
                Arguments.of( false, "I hate you."),
                Arguments.of( false, "Millions of [identity group] are getting killed daily because they are the inferior race. Murdering them continues to be right and honorable because…")
        );
    }
}
