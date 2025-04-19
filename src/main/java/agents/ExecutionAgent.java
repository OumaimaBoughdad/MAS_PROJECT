package agents;

import jade.core.Agent;
import jade.core.AID;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.*;

public class ExecutionAgent extends Agent {
    // Prioritized list of AI agents
    private static final String[] AI_AGENTS = {
            "TogetherAgent",
            "OpenRouterAgent",
            "DeepInfraAgent",
            "LangsearchAgent",
            "DeepInfraAgent"
    };

    protected void setup() {
        System.out.println("ExecutionAgent " + getAID().getName() + " is ready.");

        addBehaviour(new CyclicBehaviour(this) {
            private final Map<String, String> context = new ConcurrentHashMap<>();

            public void action() {
                ACLMessage msg = receive(MessageTemplate.MatchPerformative(ACLMessage.REQUEST));
                if (msg != null) {
                    String query = msg.getContent();
                    context.clear();
                    updateContext(query);
                    System.out.println("Processing complex query: " + query);

                    List<String> subQueries = decomposeQuery(query);
                    StringBuilder finalResult = new StringBuilder();
                    finalResult.append("=== AI-Powered Combined Results ===\n\n");

                    // Process subqueries in parallel
                    ExecutorService executor = Executors.newFixedThreadPool(subQueries.size());
                    List<Future<String>> futures = new ArrayList<>();

                    for (String subQuery : subQueries) {
                        futures.add(executor.submit(() -> {
                            String resolvedQuery = resolvePronouns(subQuery);
                            return processSubQuery(resolvedQuery);
                        }));
                    }

                    // Collect results
                    for (Future<String> future : futures) {
                        try {
                            finalResult.append(future.get(20, TimeUnit.SECONDS)).append("\n\n");
                        } catch (Exception e) {
                            finalResult.append("• Processing error: ").append(e.getMessage()).append("\n\n");
                        }
                    }

                    executor.shutdown();

                    // Send final response
                    ACLMessage reply = msg.createReply();
                    reply.setPerformative(ACLMessage.INFORM);
                    reply.setContent(finalResult.toString());
                    send(reply);
                } else {
                    block();
                }
            }

            // Context management methods remain the same
            private void updateContext(String query) {
                Matcher nameMatcher = Pattern.compile("\\b([A-Z][a-z]+\\s+[A-Z][a-z]+)\\b").matcher(query);
                if (nameMatcher.find()) {
                    context.put("current_subject", nameMatcher.group(1));
                }
            }

            private String resolvePronouns(String query) {
                return context.containsKey("current_subject") ?
                        query.replaceAll("\\b(he|she|they|it)\\b", context.get("current_subject")) :
                        query;
            }

            private List<String> decomposeQuery(String query) {
                List<String> subQueries = new ArrayList<>();
                String[] parts = query.split("(?i)\\b(and|or|but|then|also|,|;|vs\\.?|versus)\\b");
                for (String part : parts) {
                    part = part.trim();
                    if (!part.isEmpty()) subQueries.add(part);
                }
                return subQueries;
            }

            // Enhanced subquery processing
            private String processSubQuery(String subQuery) {
                if (subQuery.isEmpty()) return "";

                // Check cache first
                String cacheKey = subQuery + (context.containsKey("current_subject") ?
                        "|" + context.get("current_subject") : "");
                String cachedResponse = KnowledgeStorage.retrieve(cacheKey);
                if (cachedResponse != null) {
                    return formatResult(subQuery, cachedResponse, "Cached Knowledge");
                }

                // Query agents in parallel
                Map<String, String> resultsByAgent = new ConcurrentHashMap<>();
                CountDownLatch latch = new CountDownLatch(AI_AGENTS.length);

                for (String agentName : AI_AGENTS) {
                    new Thread(() -> {
                        try {
                            String processedQuery = preprocessForAgent(subQuery, agentName);
                            ACLMessage request = new ACLMessage(ACLMessage.REQUEST);
                            request.addReceiver(new AID(agentName, AID.ISLOCALNAME));
                            request.setContent(processedQuery);
                            send(request);

                            ACLMessage reply = blockingReceive(
                                    MessageTemplate.and(
                                            MessageTemplate.MatchPerformative(ACLMessage.INFORM),
                                            MessageTemplate.MatchSender(new AID(agentName, AID.ISLOCALNAME))
                                    ),
                                    15000 // 15 second timeout
                            );

                            if (reply != null && isValidResponse(reply.getContent(), agentName)) {
                                resultsByAgent.put(agentName, reply.getContent());
                                extractContextFromResponse(reply.getContent());
                            }
                        } catch (Exception e) {
                            System.err.println("Error querying " + agentName + ": " + e.getMessage());
                        } finally {
                            latch.countDown();
                        }
                    }).start();
                }

                try {
                    latch.await(30, TimeUnit.SECONDS); // Overall timeout
                } catch (InterruptedException e) {
                    System.err.println("Query processing interrupted");
                }

                // Compile and store results
                StringBuilder subResult = new StringBuilder();
                StringBuilder validResponses = new StringBuilder();

                // Prioritize AI responses
                for (String agentName : AI_AGENTS) {
                    String result = resultsByAgent.get(agentName);
                    if (result != null) {
                        String formatted = formatResult(subQuery, result, agentName);
                        subResult.append(formatted).append("\n");
                        validResponses.append(formatted).append("\n");
                    }
                }

                if (validResponses.length() > 0) {
                    KnowledgeStorage.store(cacheKey, validResponses.toString());
                    return subResult.toString();
                }

                return "No valid results found for: " + subQuery;
            }

            // Enhanced preprocessing
            private String preprocessForAgent(String query, String agentName) {
                String processed = query;

                switch (agentName) {
                    case "TogetherAgent":
                    case "OpenRouterAgent":
                    case "DeepInfraAgent":
                        if (!query.endsWith(" ")) {
                            processed = query + "?";
                        }
                        break;
                    case "WikipediaAgent":
                    case "DuckDuckGoAgent":
                    case "WikidataAgent":
                        processed = processed.replaceAll("^(what is|who is|when was|where is|how does|how old)\\s+", "")
                                .replaceAll("\\?$", "")
                                .trim();
                        break;
                    case "LangsearchAgent":
                        // Keep original query format
                        break;
                }

                return processed;
            }

            // Enhanced response validation
            private boolean isValidResponse(String response, String agentName) {
                if (response == null || response.trim().isEmpty()) {
                    return false;
                }

                String lowerResponse = response.toLowerCase();

                // Common invalid patterns
                if (lowerResponse.contains("error") ||
                        lowerResponse.contains("no result") ||
                        lowerResponse.contains("not found") ||
                        response.trim().length() < 20) {
                    return false;
                }

                // Agent-specific validation
                switch (agentName) {
                    case "TogetherAgent":
                    case "OpenRouterAgent":
                    case "DeepInfraAgent":
                        return !lowerResponse.matches(".*\\b(sorry|unable|cannot|don't know)\\b.*") &&
                                response.length() > 50;
                    case "LangsearchAgent":
                        return response.split("\n").length >= 3; // At least title + URL + snippet
                    default:
                        return response.length() > 30;
                }
            }

            private void extractContextFromResponse(String response) {
                if (context.containsKey("current_subject") &&
                        response.matches(".*\\b(born|age)\\b.*\\d{4}.*")) {
                    String ageInfo = response.replaceAll(".*\\b(born|age)\\b.*?(\\d{4}).*", "$2");
                    context.put(context.get("current_subject") + "_age", ageInfo);
                }
            }

            private String formatResult(String query, String response, String source) {
                String sourceName = source.replace("Agent", "");
                return String.format("• [%s] %s:\n%s", sourceName, query, response);
            }
        });
    }
}