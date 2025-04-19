package agents;

import utils.HttpHelper;

public class TestOpenRouter {
    public static void main(String[] args) {
        String prompt = "suggest some cat names ";
        String response = HttpHelper.searchExternalSource("openrouter", prompt);

        System.out.println("=== OpenRouter Response ===");
        System.out.println(response);
        System.out.println("===========================");
    }
}
