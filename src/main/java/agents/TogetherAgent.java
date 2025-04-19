package agents;

import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import utils.HttpHelper;

public class TogetherAgent extends Agent {
    protected void setup() {
        System.out.println("[READY] " + getAID().getName() + " ready for Together.ai");

        addBehaviour(new CyclicBehaviour(this) {
            public void action() {
                ACLMessage msg = receive(MessageTemplate.MatchPerformative(ACLMessage.REQUEST));
                if (msg != null) {
                    String query = msg.getContent();
                    System.out.println("[PROCESSING] Together.ai query: " + query);

                    // Use the direct TogetherAI query method instead of searchExternalSource
                    String response = HttpHelper.queryTogetherAI(query);

                    ACLMessage reply = msg.createReply();
                    reply.setPerformative(ACLMessage.INFORM);
                    reply.setContent(response);
                    send(reply);
                    System.out.println("[SENT] Response: " + response);
                } else {
                    block();
                }
            }
        });
    }
}