package agents;

import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import java.util.HashMap;
import java.util.Map;




import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

public class InternalAgent extends Agent {
    protected void setup() {
        System.out.println("InternalAgent " + getAID().getName() + " is ready.");

        addBehaviour(new CyclicBehaviour(this) {
            public void action() {
                // Handle knowledge checks
                ACLMessage queryMsg = receive(MessageTemplate.MatchPerformative(ACLMessage.QUERY_IF));
                if (queryMsg != null) {
                    String query = queryMsg.getContent();
                    String response = KnowledgeStorage.retrieve(query);

                    ACLMessage reply = queryMsg.createReply();
                    if (response != null) {
                        reply.setPerformative(ACLMessage.CONFIRM);
                        reply.setContent(response); // Return clean response without source prefix
                    } else {
                        reply.setPerformative(ACLMessage.DISCONFIRM);
                    }
                    send(reply);
                }

                // Handle knowledge storage
                ACLMessage informMsg = receive(MessageTemplate.MatchPerformative(ACLMessage.INFORM));
                if (informMsg != null) {
                    String content = informMsg.getContent();
                    String[] parts = content.split("::", 2);
                    if (parts.length == 2 && !parts[1].contains("Error fetching")) {
                        KnowledgeStorage.store(parts[0], parts[1]);
                    }
                }

                block();
            }
        });
    }
}