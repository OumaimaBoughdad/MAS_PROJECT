package agents;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import java.util.*;
import java.util.concurrent.*;

public class BrokerAgent extends Agent {
    // List of all available resource agents
    private static final String[] ALL_AGENTS = {
            "WikipediaAgent",
            "DuckDuckGoAgent",
            "WikidataAgent",
            "OpenRouterAgent",
            "LangsearchAgent",
            "TogetherAgent",  // Make sure this matches exactly with the actual agent name
            "DeepSeekAgent"
    };

    private static final String[] BOOK_AGENTS = {
            "BookSearchAgent"
    };

    private static final String[] MATH_AGENTS = {
            "WolframAlphaAgent"
    };

    // Agents that should be excluded when adding specialty agents
    private static final String[] EXCLUDED_AGENTS = {
            "WikipediaAgent",
            "DuckDuckGoAgent",
            "WikidataAgent"
    };

    protected void setup() {
        System.out.println("BrokerAgent " + getAID().getName() + " is ready.");

        addBehaviour(new CyclicBehaviour(this) {
            private final Map<String, String> contextMap = new ConcurrentHashMap<>();
            private final Set<String> activeAgents = new HashSet<>();

            public void action() {
                ACLMessage msg = receive(MessageTemplate.MatchPerformative(ACLMessage.REQUEST));
                if (msg != null) {
                    String query = msg.getContent();
                    System.out.println("Broker received query: " + query);

                    // Update context with previous interactions
                    updateContextFromQuery(query);

                    if (isComplexQuery(query)) {
                        System.out.println("Detected COMPLEX query");
                        handleComplexQuery(msg);
                    } else {
                        System.out.println("Detected SIMPLE query");
                        handleSimpleQuery(msg);
                    }
                } else {
                    block();
                }
            }

            private void updateContextFromQuery(String query) {
                // Extract named entities and store in context
                if (query.matches(".*\\b(Steven Spielberg|Albert Einstein|Elon Musk)\\b.*")) {
                    String subject = query.replaceAll(".*\\b(Steven Spielberg|Albert Einstein|Elon Musk)\\b.*", "$1");
                    contextMap.put("current_subject", subject);
                    System.out.println("Updated context with subject: " + subject);
                }
            }

            private boolean isComplexQuery(String query) {
                String lowerQuery = query.toLowerCase();
                return lowerQuery.matches(".*\\b(and|or|but|then|also|as well as|before|after|while|meanwhile)\\b.*") ||
                        lowerQuery.contains(",") ||
                        lowerQuery.contains(";") ||
                        lowerQuery.split("\\?").length > 1 ||
                        lowerQuery.matches(".*\\b(list|compare|difference|between|advantages|disadvantages|pros|cons)\\b.*") ||
                        (lowerQuery.matches(".*\\b(he|she|they|it)\\b.*") && contextMap.containsKey("current_subject"));
            }

            private void handleComplexQuery(ACLMessage originalMsg) {
                String query = originalMsg.getContent();
                String resolvedQuery = resolvePronouns(query);
                System.out.println("Processing complex query with selected agents: " + resolvedQuery);

                // Use only these three agents for complex queries
                String[] agentsToQuery = {"OpenRouterAgent", "LangsearchAgent", "TogetherAgent"};

                // Create a map to store results by agent
                Map<String, String> resultsByAgent = new ConcurrentHashMap<>();
                List<String> agentsWithTimeout = new ArrayList<>();
                List<String> agentsWithError = new ArrayList<>();

                // Query all selected agents in parallel
                ExecutorService executor = Executors.newFixedThreadPool(agentsToQuery.length);
                CountDownLatch latch = new CountDownLatch(agentsToQuery.length);

                for (String agentName : agentsToQuery) {
                    executor.execute(() -> {
                        try {
                            String processedQuery = agentSpecificPreprocessing(resolvedQuery, agentName);
                            System.out.println("Sending to " + agentName + ": " + processedQuery);

                            ACLMessage request = new ACLMessage(ACLMessage.REQUEST);
                            request.addReceiver(new AID(agentName, AID.ISLOCALNAME));
                            request.setContent(processedQuery);
                            send(request);

                            // Wait for response with timeout
                            ACLMessage reply = blockingReceive(
                                    MessageTemplate.and(
                                            MessageTemplate.MatchPerformative(ACLMessage.INFORM),
                                            MessageTemplate.MatchSender(new AID(agentName, AID.ISLOCALNAME))
                                    ),
                                    15000 // 15 seconds timeout
                            );

                            if (reply != null) {
                                System.out.println("Received from " + agentName + ": " + reply.getContent());
                                resultsByAgent.put(agentName, reply.getContent());
                            } else {
                                System.out.println("Timeout waiting for " + agentName);
                                agentsWithTimeout.add(agentName);
                            }
                        } catch (Exception e) {
                            System.out.println("Error with " + agentName + ": " + e.getMessage());
                            agentsWithError.add(agentName);
                        } finally {
                            latch.countDown();
                        }
                    });
                }

                try {
                    if (!latch.await(20, TimeUnit.SECONDS)) {
                        System.out.println("Overall query timeout occurred");
                    }
                } catch (InterruptedException e) {
                    System.out.println("Query interrupted");
                } finally {
                    executor.shutdown();
                }

                // Process and combine results
                StringBuilder combinedResults = new StringBuilder();
                combinedResults.append("=== Combined Results from Specialized Agents ===\n\n");

                for (String agentName : agentsToQuery) {
                    String result = resultsByAgent.get(agentName);
                    if (result != null && isValidResponse(result, agentName)) {
                        combinedResults.append(formatResponse(agentName, result)).append("\n\n");
                    }
                }

                // Send combined response
                ACLMessage reply = originalMsg.createReply();
                reply.setPerformative(ACLMessage.INFORM);

                if (combinedResults.length() > 0) {
                    reply.setContent(combinedResults.toString());
                    // Cache the successful response
                    KnowledgeStorage.store(resolvedQuery, combinedResults.toString());
                } else {
                    // Provide detailed error information
                    StringBuilder errorMsg = new StringBuilder("No valid results found from specialized agents. ");
                    if (!agentsWithTimeout.isEmpty()) {
                        errorMsg.append("Timeouts from: ").append(agentsWithTimeout).append(". ");
                    }
                    if (!agentsWithError.isEmpty()) {
                        errorMsg.append("Errors from: ").append(agentsWithError).append(". ");
                    }
                    reply.setContent(errorMsg.toString());
                }

                send(reply);
            }

            private String resolvePronouns(String query) {
                if (contextMap.containsKey("current_subject")) {
                    String resolved = query.replaceAll("\\b(he|she|they|it)\\b", contextMap.get("current_subject"));
                    System.out.println("Resolved pronouns: " + resolved);
                    return resolved;
                }
                return query;
            }

            private void handleSimpleQuery(ACLMessage originalMsg) {
                String query = originalMsg.getContent();
                String resolvedQuery = resolvePronouns(query);
                System.out.println("Processing simple query: " + resolvedQuery);

                // First check internal knowledge
                String cachedResponse = KnowledgeStorage.retrieve(resolvedQuery);
                if (cachedResponse != null) {
                    System.out.println("Returning cached response");
                    ACLMessage reply = originalMsg.createReply();
                    reply.setPerformative(ACLMessage.INFORM);
                    reply.setContent("Cached Result:\n" + cachedResponse);
                    send(reply);
                    return;
                }

                // Determine which agents should process this query
                String[] agentsToQuery = determineAgentsForQuery(resolvedQuery);
                System.out.println("Selected agents: " + Arrays.toString(agentsToQuery));

                // Create a map to store results by agent
                Map<String, String> resultsByAgent = new ConcurrentHashMap<>();
                List<String> agentsWithTimeout = new ArrayList<>();
                List<String> agentsWithError = new ArrayList<>();

                // Query all resources in parallel
                ExecutorService executor = Executors.newFixedThreadPool(agentsToQuery.length);
                CountDownLatch latch = new CountDownLatch(agentsToQuery.length);

                for (String agentName : agentsToQuery) {
                    executor.execute(() -> {
                        try {
                            String processedQuery = agentSpecificPreprocessing(resolvedQuery, agentName);
                            System.out.println("Sending to " + agentName + ": " + processedQuery);

                            ACLMessage request = new ACLMessage(ACLMessage.REQUEST);
                            request.addReceiver(new AID(agentName, AID.ISLOCALNAME));
                            request.setContent(processedQuery);
                            send(request);

                            // Wait for response with timeout
                            ACLMessage reply = blockingReceive(
                                    MessageTemplate.and(
                                            MessageTemplate.MatchPerformative(ACLMessage.INFORM),
                                            MessageTemplate.MatchSender(new AID(agentName, AID.ISLOCALNAME))
                                    ),
                                    15000 // 15 seconds timeout
                            );

                            if (reply != null) {
                                System.out.println("Received from " + agentName + ": " + reply.getContent());
                                resultsByAgent.put(agentName, reply.getContent());
                            } else {
                                System.out.println("Timeout waiting for " + agentName);
                                agentsWithTimeout.add(agentName);
                            }
                        } catch (Exception e) {
                            System.out.println("Error with " + agentName + ": " + e.getMessage());
                            agentsWithError.add(agentName);
                        } finally {
                            latch.countDown();
                        }
                    });
                }

                try {
                    if (!latch.await(20, TimeUnit.SECONDS)) {
                        System.out.println("Overall query timeout occurred");
                    }
                } catch (InterruptedException e) {
                    System.out.println("Query interrupted");
                } finally {
                    executor.shutdown();
                }

                // Process and combine results
                StringBuilder combinedResults = new StringBuilder();
                combinedResults.append("=== Context-Aware Combined Results ===\n\n");
                StringBuilder allValidResponses = new StringBuilder();

                for (String agentName : agentsToQuery) {
                    String result = resultsByAgent.get(agentName);
                    if (result != null) {
                        String formatted = formatResponse(agentName, result);
                        if (isValidResponse(result, agentName)) {
                            combinedResults.append(formatted).append("\n\n");
                            allValidResponses.append(formatted).append("\n\n");
                            updateContextFromResponse(result);
                            activeAgents.add(agentName); // Mark as active
                        } else {
                            System.out.println("Filtered out invalid response from " + agentName);
                        }
                    }
                }

                // Store all valid responses
                if (allValidResponses.length() > 0) {
                    KnowledgeStorage.store(resolvedQuery, allValidResponses.toString());
                }

                // Send combined response
                ACLMessage reply = originalMsg.createReply();
                reply.setPerformative(ACLMessage.INFORM);

                if (combinedResults.length() > 0) {
                    reply.setContent(combinedResults.toString());
                } else {
                    // Provide detailed error information
                    StringBuilder errorMsg = new StringBuilder("No valid results found. ");
                    if (!agentsWithTimeout.isEmpty()) {
                        errorMsg.append("Timeouts from: ").append(agentsWithTimeout).append(". ");
                    }
                    if (!agentsWithError.isEmpty()) {
                        errorMsg.append("Errors from: ").append(agentsWithError).append(". ");
                    }
                    errorMsg.append("Active agents: ").append(activeAgents);
                    reply.setContent(errorMsg.toString());
                }

                send(reply);
            }

            private String agentSpecificPreprocessing(String query, String agentName) {
                // Special preprocessing for TogetherAgent
                if (agentName.equals("TogetherAgent")) {
                    System.out.println("Special preprocessing for TogetherAgent");
                    // TogetherAgent prefers complete questions
                    return query.endsWith("?") ? query : query + "?";
                }

                String processed = query;

                if (agentName.equals("WikipediaAgent") ||
                        agentName.equals("DuckDuckGoAgent") ||
                        agentName.equals("WikidataAgent")) {

                    processed = processed.replaceAll("^(what is|who is|when was|where is|how does|how old)\\s+", "")
                            .replaceAll("\\?$", "")
                            .trim();
                }

                if (agentName.equals("WolframAlphaAgent") && isMathQuery(query)) {
                    processed = processed.replaceAll("\\b(calculate|compute|solve)\\b", "")
                            .replaceAll("\\?", "")
                            .trim();
                }

                return processed;
            }

            private void updateContextFromResponse(String response) {
                if (response.matches(".*\\b(born|age)\\b.*\\d{4}.*")) {
                    String ageInfo = response.replaceAll(".*\\b(born|age)\\b.*?(\\d{4}).*", "$1 $2");
                    if (contextMap.containsKey("current_subject")) {
                        contextMap.put(contextMap.get("current_subject") + "_age", ageInfo);
                        System.out.println("Updated context with age info: " + ageInfo);
                    }
                }
            }

            private String[] determineAgentsForQuery(String query) {
                Set<String> agents = new HashSet<>(Arrays.asList(ALL_AGENTS));

                // Special handling for TogetherAgent - only use for certain queries
                if (!isQuerySuitableForTogetherAgent(query)) {
                    agents.remove("TogetherAgent");
                    System.out.println("Query not suitable for TogetherAgent");
                }

                if (isBookQuery(query)) {
                    System.out.println("Adding book agents");
                    for (String bookAgent : BOOK_AGENTS) {
                        if (!Arrays.asList(EXCLUDED_AGENTS).contains(bookAgent)) {
                            agents.add(bookAgent);
                        }
                    }
                }

                if (isMathQuery(query)) {
                    System.out.println("Adding math agents");
                    for (String mathAgent : MATH_AGENTS) {
                        if (!Arrays.asList(EXCLUDED_AGENTS).contains(mathAgent)) {
                            agents.add(mathAgent);
                        }
                    }
                }

                return agents.toArray(new String[0]);
            }

            private boolean isQuerySuitableForTogetherAgent(String query) {
                // TogetherAgent works best with natural language questions
                return query.trim().endsWith("?") ||
                        query.matches(".*\\b(how|why|what|when|where|who|explain|describe)\\b.*");
            }

            private boolean isBookQuery(String query) {
                String lowerQuery = query.toLowerCase();
                return lowerQuery.matches(".*\\b(book|books|author|authors|novel|novels|publish|published|publication|title|isbn|chapter|pages)\\b.*");
            }

            private boolean isMathQuery(String query) {
                String lowerQuery = query.toLowerCase();
                return lowerQuery.matches(".*\\d+\\s*[+\\-*/%^=]\\s*\\d+.*") ||
                        lowerQuery.matches(".*\\b(calculate|compute|solve|equation|math|sum of|product of|derivative|integral|algebra|geometry|trigonometry|calculus|formula|theorem)\\b.*") ||
                        lowerQuery.matches(".*\\b(sin|cos|tan|log|ln|sqrt|root|square|cube|factorial|permutation|combination)\\b.*");
            }

            private String formatResponse(String agentName, String content) {
                String sourceName = agentName.replace("Agent", "");
                return "[" + sourceName + " Result]\n" + content;
            }

            private boolean isValidResponse(String response, String agentName) {
                if (response == null || response.trim().isEmpty()) {
                    return false;
                }

                // Special validation for TogetherAgent
                if (agentName.equals("TogetherAgent")) {
                    return !response.toLowerCase().contains("i cannot answer") &&
                            !response.toLowerCase().contains("i don't know") &&
                            response.length() > 20;
                }

                // General validation for other agents
                return !response.toLowerCase().contains("error") &&
                        !response.toLowerCase().contains("no result") &&
                        !response.toLowerCase().contains("not found") &&
                        response.length() > 10;
            }
        });
    }
}