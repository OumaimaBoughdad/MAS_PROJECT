package agents;

import jade.core.Agent;
import jade.core.AID;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import java.util.*;
import java.util.concurrent.*;

public class ExecutionAgent extends Agent {
    private static final String[] RESOURCE_AGENTS = {
            "WikipediaAgent",
            "DuckDuckGoAgent",
            "BookSearchAgent",
            "OpenRouterAgent",
            "WolframAlphaAgent"
    };

    protected void setup() {
        System.out.println("ExecutionAgent " + getAID().getName() + " is ready.");

        addBehaviour(new CyclicBehaviour(this) {
            public void action() {
                ACLMessage msg = receive(MessageTemplate.MatchPerformative(ACLMessage.REQUEST));
                if (msg != null) {
                    String query = msg.getContent();
                    System.out.println("ExecutionAgent processing complex query: " + query);

                    String[] subQueries = decomposeQuery(query);
                    StringBuilder finalResult = new StringBuilder();
                    finalResult.append("=== Combined Results ===\n\n");

                    ExecutorService executor = Executors.newFixedThreadPool(subQueries.length);
                    List<Future<String>> futures = new ArrayList<>();

                    for (String subQuery : subQueries) {
                        futures.add(executor.submit(() -> processSubQuery(subQuery.trim())));
                    }

                    for (Future<String> future : futures) {
                        try {
                            finalResult.append(future.get(10, TimeUnit.SECONDS)).append("\n\n");
                        } catch (Exception e) {
                            finalResult.append("• Error processing part of query\n\n");
                        }
                    }

                    executor.shutdown();

                    ACLMessage response = new ACLMessage(ACLMessage.INFORM);
                    response.addReceiver(msg.getSender());
                    response.setContent(finalResult.toString());
                    send(response);
                } else {
                    block();
                }
            }

            private String[] decomposeQuery(String query) {
                return query.split("(?i)( and |, |\\? |; | vs\\.? | versus )");
            }

            private String processSubQuery(String subQuery) {
                if (subQuery.isEmpty()) return "";

                // Check cache first
                String cachedResponse = KnowledgeStorage.retrieve(subQuery);
                if (cachedResponse != null) {
                    return formatResult(subQuery, cachedResponse, "Cached Knowledge");
                }

                // Query all sources in parallel
                ExecutorService executor = Executors.newFixedThreadPool(RESOURCE_AGENTS.length);
                List<Future<String>> futures = new ArrayList<>();
                StringBuilder subResult = new StringBuilder();

                for (String agentName : RESOURCE_AGENTS) {
                    futures.add(executor.submit(() -> {
                        try {
                            ACLMessage request = new ACLMessage(ACLMessage.REQUEST);
                            request.addReceiver(new AID(agentName, AID.ISLOCALNAME));
                            request.setContent(subQuery);
                            send(request);

                            ACLMessage reply = blockingReceive(
                                    MessageTemplate.and(
                                            MessageTemplate.MatchPerformative(ACLMessage.INFORM),
                                            MessageTemplate.MatchSender(new AID(agentName, AID.ISLOCALNAME))
                                    ),
                                    5000
                            );
                            return reply != null ? reply.getContent() : null;
                        } catch (Exception e) {
                            return null;
                        }
                    }));
                }

                // Collect results for this subquery
                for (int i = 0; i < futures.size(); i++) {
                    try {
                        String result = futures.get(i).get(6, TimeUnit.SECONDS);
                        if (result != null) {
                            subResult.append(formatResult(subQuery, result, RESOURCE_AGENTS[i]))
                                    .append("\n");
                            // Store first successful response
                            if (cachedResponse == null) {
                                KnowledgeStorage.store(subQuery, result);
                            }
                        }
                    } catch (Exception e) {
                        // Ignore timeouts
                    }
                }

                executor.shutdown();
                return subResult.toString();
            }

            private String formatResult(String query, String response, String source) {
                String sourceName = source.replace("Agent", "");
                return String.format("• [%s] %s:\n%s", sourceName, query, response);
            }
        });
    }
}
