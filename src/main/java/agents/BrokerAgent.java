package agents;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

public class BrokerAgent extends Agent {
    protected void setup() {
        System.out.println("BrokerAgent " + getAID().getName() + " is ready.");

        // Main behavior to process incoming REQUEST messages from users
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
                // Check for multiple clauses
                String lowerQuery = query.toLowerCase();
                return lowerQuery.contains(" and ") ||
                        lowerQuery.contains(",") ||
                        lowerQuery.split("\\?").length > 1 ||
                        lowerQuery.matches(".*(list|compare|difference|between).*");
            }

            private void handleSimpleQuery(ACLMessage originalMsg) {
                String query = originalMsg.getContent();

                // First check internal knowledge
                ACLMessage checkInternal = new ACLMessage(ACLMessage.QUERY_IF);
                checkInternal.addReceiver(getAID("InternalAgent"));
                checkInternal.setContent(query);
                send(checkInternal);

                // Wait for response from InternalAgent
                MessageTemplate mt = MessageTemplate.or(
                        MessageTemplate.MatchPerformative(ACLMessage.CONFIRM),
                        MessageTemplate.MatchPerformative(ACLMessage.DISCONFIRM)
                );

                ACLMessage internalReply = blockingReceive(mt, 1000);

                if (internalReply != null && internalReply.getPerformative() == ACLMessage.CONFIRM) {
                    // Found in internal knowledge base, forward result to user
                    ACLMessage reply = originalMsg.createReply();
                    reply.setPerformative(ACLMessage.INFORM);
                    reply.setContent("Internal Result:\n" + internalReply.getContent());
                    send(reply);

                } else {
                    // Not found in internal knowledge, select best external agent
                    String bestAgent = selectBestResourceAgent(query);
                    System.out.println("Not found internally. Selected " + bestAgent + " for: " + query);

                    // Forward to selected external agent
                    ACLMessage externalRequest = new ACLMessage(ACLMessage.REQUEST);
                    externalRequest.addReceiver(new AID(bestAgent, AID.ISLOCALNAME));
                    externalRequest.setContent(query);
                    send(externalRequest);

                    // Wait for response from external agent
                    ACLMessage externalReply = blockingReceive(
                            MessageTemplate.MatchPerformative(ACLMessage.INFORM),
                            5000
                    );

                    if (externalReply != null) {
                        // Forward result to user
                        ACLMessage userReply = originalMsg.createReply();
                        userReply.setPerformative(ACLMessage.INFORM);
                        userReply.setContent(externalReply.getContent());
                        send(userReply);

                        // Store in cache
                        ACLMessage store = new ACLMessage(ACLMessage.INFORM);
                        store.addReceiver(getAID("InternalAgent"));
                        store.setContent(query + "::" + externalReply.getContent());
                        send(store);
                    } else {
                        // No response from external agent
                        ACLMessage errorReply = originalMsg.createReply();
                        errorReply.setPerformative(ACLMessage.INFORM);
                        errorReply.setContent("Sorry, no results found for your query.");
                        send(errorReply);
                    }
                }
            }

            private void handleComplexQuery(ACLMessage originalMsg) {
                String query = originalMsg.getContent();

                // Forward to ExecutionAgent with original sender info
                ACLMessage forward = new ACLMessage(ACLMessage.REQUEST);
                forward.addReceiver(getAID("ExecutionAgent"));
                forward.setContent(query);
                send(forward);

                // Wait for response from ExecutionAgent with a longer timeout
                ACLMessage executionReply = blockingReceive(
                        MessageTemplate.MatchPerformative(ACLMessage.INFORM),
                        15000  // Longer timeout for complex queries
                );

                if (executionReply != null) {
                    // Forward result to original user
                    ACLMessage userReply = originalMsg.createReply();
                    userReply.setPerformative(ACLMessage.INFORM);
                    userReply.setContent(executionReply.getContent());
                    send(userReply);
                    System.out.println("Forwarded complex query results to user");
                } else {
                    // No response from ExecutionAgent
                    ACLMessage errorReply = originalMsg.createReply();
                    errorReply.setPerformative(ACLMessage.INFORM);
                    errorReply.setContent("Sorry, could not process your complex query.");
                    send(errorReply);
                    System.out.println("No response from ExecutionAgent - timeout");
                }
            }

            private String selectBestResourceAgent(String query) {
                // Simple heuristic for resource selection
                String lowerQuery = query.toLowerCase();
                if (lowerQuery.matches(".*(book|author|novel|publication).*")) {
                    return "BookSearchAgent";
                } else if (lowerQuery.matches(".*(what is|who is|define|explain).*")) {
                    return "WikipediaAgent";
                } else if (lowerQuery.matches(".*(explain|summarize|write|generate|why|how).*")) {
                    return "DeepSeekAgent";
                } else {
                    return "DuckDuckGoAgent"; // Default fallback
                }
            }

        });
    }
}