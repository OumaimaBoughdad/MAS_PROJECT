package agents;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

public class BrokerAgent extends Agent {
    // List of all available resource agents
    private static final String[] RESOURCE_AGENTS = {
            "WikipediaAgent",
            "DuckDuckGoAgent",
            "BookSearchAgent",
            "OpenRouterAgent",
            "WolframAlphaAgent"
    };

    protected void setup() {
        System.out.println("BrokerAgent " + getAID().getName() + " is ready.");

        addBehaviour(new CyclicBehaviour(this) {
            public void action() {
                ACLMessage msg = receive(MessageTemplate.MatchPerformative(ACLMessage.REQUEST));
                if (msg != null) {
                    String query = msg.getContent();
                    System.out.println("Broker received query: " + query);

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

            private boolean isComplexQuery(String query) {
                String lowerQuery = query.toLowerCase();
                return lowerQuery.matches(".*\\b(and|or|but|then|also|as well as|before|after|while|meanwhile)\\b.*") ||
                        lowerQuery.contains(",") ||
                        lowerQuery.contains(";") ||
                        lowerQuery.split("\\?").length > 1 ||
                        lowerQuery.matches(".*\\b(list|compare|difference|between|advantages|disadvantages|pros|cons)\\b.*");
            }

            private void handleSimpleQuery(ACLMessage originalMsg) {
                String query = originalMsg.getContent();

                // First check internal knowledge
                String cachedResponse = KnowledgeStorage.retrieve(query);
                if (cachedResponse != null) {
                    ACLMessage reply = originalMsg.createReply();
                    reply.setPerformative(ACLMessage.INFORM);
                    reply.setContent("Cached Result:\n" + cachedResponse);
                    send(reply);
                    return;
                }

                // Query all resources in parallel
                ExecutorService executor = Executors.newFixedThreadPool(RESOURCE_AGENTS.length);
                List<Future<String>> futures = new ArrayList<>();

                for (String agentName : RESOURCE_AGENTS) {
                    futures.add(executor.submit(() -> {
                        try {
                            ACLMessage request = new ACLMessage(ACLMessage.REQUEST);
                            request.addReceiver(new AID(agentName, AID.ISLOCALNAME));
                            request.setContent(query);
                            send(request);

                            ACLMessage reply = blockingReceive(
                                    MessageTemplate.and(
                                            MessageTemplate.MatchPerformative(ACLMessage.INFORM),
                                            MessageTemplate.MatchSender(new AID(agentName, AID.ISLOCALNAME))
                                    ),
                                    5000
                            );
                            return reply != null ? formatResponse(agentName, reply.getContent()) : null;
                        } catch (Exception e) {
                            return null;
                        }
                    }));
                }

                // Collect results
                StringBuilder combinedResults = new StringBuilder();
                combinedResults.append("=== Combined Results from All Sources ===\n\n");

                for (int i = 0; i < futures.size(); i++) {
                    try {
                        String result = futures.get(i).get(6, TimeUnit.SECONDS); // Slightly longer timeout
                        if (result != null) {
                            combinedResults.append(result).append("\n\n");
                            // Store the first successful response in cache
                            if (cachedResponse == null) {
                                KnowledgeStorage.store(query, result);
                            }
                        }
                    } catch (Exception e) {
                        // Ignore timeouts or errors for individual sources
                    }
                }

                executor.shutdown();

                // Send combined response
                ACLMessage reply = originalMsg.createReply();
                reply.setPerformative(ACLMessage.INFORM);
                if (combinedResults.length() > 0) {
                    reply.setContent(combinedResults.toString());
                } else {
                    reply.setContent("Sorry, no results found from any sources.");
                }
                send(reply);
            }

            private String formatResponse(String agentName, String content) {
                String sourceName = agentName.replace("Agent", "");
                return "[" + sourceName + " Result]\n" + content;
            }

            private void handleComplexQuery(ACLMessage originalMsg) {
                // Forward to ExecutionAgent as before
                ACLMessage forward = new ACLMessage(ACLMessage.REQUEST);
                forward.addReceiver(getAID("ExecutionAgent"));
                forward.setContent(originalMsg.getContent());
                send(forward);

                ACLMessage executionReply = blockingReceive(
                        MessageTemplate.MatchPerformative(ACLMessage.INFORM),
                        15000
                );

                ACLMessage userReply = originalMsg.createReply();
                userReply.setPerformative(ACLMessage.INFORM);
                userReply.setContent(executionReply != null ?
                        executionReply.getContent() :
                        "Sorry, could not process your complex query.");
                send(userReply);
            }
        });
    }
}


