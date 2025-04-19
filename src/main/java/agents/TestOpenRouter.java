package agents;

import utils.HttpHelper;

public class TestOpenRouter {
    public static void main(String[] args) throws Exception {
        String prompt = "what do you mean by featuring";
        String response = HttpHelper.searchExternalSource("openrouter", prompt);
        String rsponse2 = HttpHelper.queryDeepInfra(prompt);


        System.out.println("=== OpenRouter Response ===");
        System.out.println(response);
        System.out.println("===========================");
        System.out.println(rsponse2);
    }
}
