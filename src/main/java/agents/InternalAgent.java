package agents;

import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import java.util.HashMap;
import java.util.Map;

public class InternalAgent extends Agent {
    private Map<String, String> knowledgeBase;

    protected void setup() {
        System.out.println("InternalAgent " + getAID().getName() + " is ready.");

        // Initialize with some sample data
        knowledgeBase = new HashMap<>();
        knowledgeBase.put("capital of france", "Paris");
        knowledgeBase.put("population of france", "67 million (2023 estimate)");

        addBehaviour(new CyclicBehaviour(this) {
            public void action() {
                // Check if we have the answer
                ACLMessage msg = receive(MessageTemplate.MatchPerformative(ACLMessage.QUERY_IF));
                if (msg != null) {
                    String query = msg.getContent().toLowerCase();
                    System.out.println("InternalAgent checking for: " + query);

                    ACLMessage reply;
                    if (knowledgeBase.containsKey(query)) {
                        // Found in cache
                        reply = new ACLMessage(ACLMessage.CONFIRM);
                        reply.setContent(knowledgeBase.get(query));
                        System.out.println("Found in cache: " + knowledgeBase.get(query));
                    } else {
                        // Not found
                        reply = new ACLMessage(ACLMessage.DISCONFIRM);
                        System.out.println("Not found in cache");
                    }
                    reply.addReceiver(msg.getSender());
                    send(reply);
                } else {
                    block();
                }
            }
        });

        // Behavior to store new answers
        addBehaviour(new CyclicBehaviour(this) {
            public void action() {
                ACLMessage msg = receive(MessageTemplate.MatchPerformative(ACLMessage.INFORM));
                if (msg != null) {
                    String content = msg.getContent();
                    // Expecting format: "query::answer"
                    String[] parts = content.split("::");
                    if (parts.length == 2) {
                        knowledgeBase.put(parts[0].toLowerCase(), parts[1]);
                        System.out.println("Stored in cache: " + parts[0] + " -> " + parts[1]);
                    }
                } else {
                    block();
                }
            }
        });
    }
}