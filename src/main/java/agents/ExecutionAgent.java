package agents;

import jade.core.Agent;
import jade.core.AID;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.ParallelBehaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

public class ExecutionAgent extends Agent {
    private List<String> partialResults = new ArrayList<>();
    private int expectedResponses = 0;
    private ACLMessage originalRequest;

    protected void setup() {
        System.out.println("ExecutionAgent " + getAID().getName() + " is ready.");

        addBehaviour(new CyclicBehaviour(this) {
            public void action() {
                ACLMessage msg = receive(MessageTemplate.MatchPerformative(ACLMessage.REQUEST));
                if (msg != null) {
                    originalRequest = msg;
                    String query = msg.getContent();
                    System.out.println("ExecutionAgent processing: " + query);

                    // Enhanced query decomposition
                    String[] subQueries = decomposeQuery(query);
                    expectedResponses = subQueries.length;
                    partialResults.clear();

                    // Process each subquery
                    for (String subQuery : subQueries) {
                        processSubQuery(subQuery.trim());
                    }

                    // Once all subqueries have been processed, send the combined response
                    sendCombinedResponse();
                } else {
                    block();
                }
            }

            private String[] decomposeQuery(String query) {
                // Split by AND, commas, or multiple questions
                return query.split("( and |, |\\? |; )");
            }

            private void processSubQuery(String subQuery) {
                System.out.println("Processing sub-query: " + subQuery);

                // First check internal knowledge
                ACLMessage checkInternal = new ACLMessage(ACLMessage.QUERY_IF);
                AID internalAgentAID = new AID("InternalAgent", AID.ISLOCALNAME);
                checkInternal.addReceiver(internalAgentAID);
                checkInternal.setContent(subQuery);
                send(checkInternal);

                // Wait for response with timeout
                MessageTemplate mt = MessageTemplate.or(
                        MessageTemplate.MatchPerformative(ACLMessage.CONFIRM),
                        MessageTemplate.MatchPerformative(ACLMessage.DISCONFIRM)
                );
                ACLMessage reply = blockingReceive(mt, 2000);

                if (reply != null && reply.getPerformative() == ACLMessage.CONFIRM) {
                    // Found in cache
                    synchronized (partialResults) {
                        partialResults.add("• " + subQuery + ": " + reply.getContent());
                        System.out.println("Found result for '" + subQuery + "' in internal knowledge base");
                    }
                } else {
                    // Not found - select best external resource
                    String bestAgent = selectBestResourceAgent(subQuery);
                    System.out.println("Selected " + bestAgent + " for: " + subQuery);

                    ACLMessage askResource = new ACLMessage(ACLMessage.REQUEST);
                    AID bestAgentAID = new AID(bestAgent, AID.ISLOCALNAME);
                    askResource.addReceiver(bestAgentAID);
                    askResource.setContent(subQuery);
                    send(askResource);

                    // Wait for response with timeout
                    reply = blockingReceive(MessageTemplate.MatchPerformative(ACLMessage.INFORM), 5000);
                    if (reply != null) {
                        synchronized (partialResults) {
                            partialResults.add("• " + subQuery + ": " + reply.getContent());
                            System.out.println("Received result for '" + subQuery + "' from " + bestAgent);
                        }

                        // Store in cache
                        ACLMessage store = new ACLMessage(ACLMessage.INFORM);
                        store.addReceiver(internalAgentAID);
                        store.setContent(subQuery + "::" + reply.getContent());
                        send(store);
                    } else {
                        // No response from external agent
                        synchronized (partialResults) {
                            partialResults.add("• " + subQuery + ": No result found");
                            System.out.println("No response received for '" + subQuery + "'");
                        }
                    }
                }
            }

            private String selectBestResourceAgent(String query) {
                // Simple heuristic for resource selection
                if (query.matches(".*(book|author|novel|publication).*")) {
                    return "BookSearchAgent";
                } else if (query.matches(".*(what is|who is|define|explain).*")) {
                    return "WikipediaAgent";
                } else {
                    return "DuckDuckGoAgent"; // Default fallback
                }
            }

            private void sendCombinedResponse() {
                StringBuilder finalResult = new StringBuilder();
                finalResult.append("=== Combined Results ===\n");
                for (String res : partialResults) {
                    finalResult.append(res).append("\n\n");
                }

                // Create response message to sender of original request
                ACLMessage response = new ACLMessage(ACLMessage.INFORM);
                response.addReceiver(originalRequest.getSender());
                response.setContent(finalResult.toString());
                send(response);
                System.out.println("Complex query processing complete and response sent to " +
                        originalRequest.getSender().getName());
            }
        });
    }
}