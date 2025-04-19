

import utils.HttpHelper;

public class TestTogetherAI {
    public static void main(String[] args) {
        String prompt = "the tall of shah rukh khan";

        // Use "openrouter" as the entry point, fallback is automatic
        String response = HttpHelper.searchExternalSource("openrouter", prompt);

        System.out.println("=== Together.ai (Fallback) Response ===");
        System.out.println(response);
        System.out.println("========================================");
    }
}
