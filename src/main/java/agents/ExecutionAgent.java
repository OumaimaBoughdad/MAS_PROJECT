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
    private static final String[] RESOURCE_AGENTS = {
            "WikipediaAgent",
            "DuckDuckGoAgent",
            "BookSearchAgent",
            "OpenRouterAgent",
            "WikidataAgent",
            "LangsearchAgent",
            "TogetherAgent",
            "WolframAlphaAgent"
    };

    protected void setup() {
        System.out.println("ExecutionAgent " + getAID().getName() + " is ready.");

        addBehaviour(new CyclicBehaviour(this) {
            private final Map<String, String> context = new ConcurrentHashMap<>();

            public void action() {
                ACLMessage msg = receive(MessageTemplate.MatchPerformative(ACLMessage.REQUEST));
                if (msg != null) {
                    String query = msg.getContent();
                    context.clear(); // <<<<<<<<<<<< RESET CONTEXT FOR EACH NEW QUERY
                    updateContext(query);
                    System.out.println("ExecutionAgent processing complex query: " + query);

                    updateContext(query);
                    List<String> subQueries = decomposeQuery(query);
                    StringBuilder finalResult = new StringBuilder();
                    finalResult.append("=== Context-Aware Combined Results ===\n\n");
                    ExecutorService executor = Executors.newFixedThreadPool(subQueries.size());
                    List<Future<String>> futures = new ArrayList<>();

                    for (String subQuery : subQueries) {
                        futures.add(executor.submit(() -> {
                            String resolvedQuery = resolvePronouns(subQuery);
                            return processSubQuery(resolvedQuery);
                        }));
                    }

                    for (Future<String> future : futures) {
                        try {
                            finalResult.append(future.get(15, TimeUnit.SECONDS)).append("\n\n");
                        } catch (Exception e) {
                            finalResult.append("• Error processing part of query: ").append(e.getMessage()).append("\n\n");
                        }
                    }

                    executor.shutdown();

                    ACLMessage reply = msg.createReply();
                    reply.setPerformative(ACLMessage.INFORM);
                    reply.setContent(finalResult.toString());
                    send(reply);
                } else {
                    block();
                }
            }

            private void updateContext(String query) {
                Matcher nameMatcher = Pattern.compile("\\b([A-Z][a-z]+\\s+[A-Z][a-z]+)\\b").matcher(query);
                if (nameMatcher.find()) {
                    String subject = nameMatcher.group(1);
                    context.put("current_subject", subject);
                }
            }

            private String resolvePronouns(String query) {
                if (context.containsKey("current_subject")) {
                    return query.replaceAll("\\b(he|she|they|it)\\b", context.get("current_subject"));
                }
                return query;
            }

            private List<String> decomposeQuery(String query) {
                List<String> subQueries = new ArrayList<>();
                String[] parts = query.split("(?i)\\b(and|or|but|then|also|,|;|vs\\.?|versus)\\b");
                for (String part : parts) {
                    part = part.trim();
                    if (!part.isEmpty()) {
                        subQueries.add(part);
                    }
                }
                return subQueries;
            }

            private String processSubQuery(String subQuery) {
                if (subQuery.isEmpty()) return "";

                String cacheKey = subQuery + (context.containsKey("current_subject") ?
                        "|" + context.get("current_subject") : "");
                String cachedResponse = KnowledgeStorage.retrieve(cacheKey);
                if (cachedResponse != null) {
                    return formatResult(subQuery, cachedResponse, "Cached Knowledge");
                }

                Map<String, String> resultsByAgent = new ConcurrentHashMap<>();
                CountDownLatch latch = new CountDownLatch(RESOURCE_AGENTS.length);

                for (String agentName : RESOURCE_AGENTS) {
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
                                    10000
                            );

                            if (reply != null) {
                                resultsByAgent.put(agentName, reply.getContent());
                                extractContextFromResponse(reply.getContent());
                            }
                        } catch (Exception e) {
                            System.out.println("Error querying " + agentName + ": " + e.getMessage());
                        } finally {
                            latch.countDown();
                        }
                    }).start();
                }

                try {
                    latch.await(20, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    System.out.println("Subquery processing interrupted");
                }

                StringBuilder subResult = new StringBuilder();
                StringBuilder validResponses = new StringBuilder();

                for (String agentName : RESOURCE_AGENTS) {
                    String result = resultsByAgent.get(agentName);
                    if (result != null && isValidResponse(result)) {
                        String formatted = formatResult(subQuery, result, agentName);
                        subResult.append(formatted).append("\n");
                        validResponses.append(formatted).append("\n");
                    }
                }

                if (validResponses.length() > 0) {
                    KnowledgeStorage.store(cacheKey, validResponses.toString());
                }

                return subResult.length() > 0 ? subResult.toString() :
                        "No valid results found for: " + subQuery;
            }

            private String preprocessForAgent(String query, String agentName) {
                String processed = query;

                if (agentName.equals("WikipediaAgent") ||
                        agentName.equals("DuckDuckGoAgent") ||
                        agentName.equals("WikidataAgent")) {

                    processed = processed.replaceAll("^(what is|who is|when was|where is|how does|how old)\\s+", "")
                            .replaceAll("\\?$", "")
                            .trim();
                }

                if (agentName.equals("WolframAlphaAgent") &&
                        query.matches(".*\\b(calculate|compute|solve)\\b.*")) {
                    processed = processed.replaceAll("\\b(calculate|compute|solve)\\b", "")
                            .trim();
                }

                return processed;
            }

            private void extractContextFromResponse(String response) {
                if (response.matches(".*\\b(born|age)\\b.*\\d{4}.*") &&
                        context.containsKey("current_subject")) {
                    String ageInfo = response.replaceAll(".*\\b(born|age)\\b.*?(\\d{4}).*", "$2");
                    context.put(context.get("current_subject") + "_age", ageInfo);
                }
            }

            private boolean isValidResponse(String response) {
                return response != null &&
                        !response.toLowerCase().contains("error") &&
                        !response.toLowerCase().contains("no result") &&
                        !response.trim().isEmpty() &&
                        response.length() > 15;
            }

            private String formatResult(String query, String response, String source) {
                String sourceName = source.replace("Agent", "");
                return String.format("• [%s] %s:\n%s", sourceName, query, response);
            }



        });
    }
}
