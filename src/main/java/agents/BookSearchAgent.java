package agents;

import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import utils.HttpHelper;

public class BookSearchAgent extends Agent {
    protected void setup() {
        System.out.println(getAID().getName() + " is ready for book searches.");

        addBehaviour(new CyclicBehaviour(this) {
            public void action() {
                ACLMessage msg = receive(MessageTemplate.MatchPerformative(ACLMessage.REQUEST));
                if (msg != null) {
                    String query = msg.getContent();
                    System.out.println("Searching Google Books for: " + query);
                    String response = HttpHelper.searchExternalSource("googlebooks", query);

                    ACLMessage reply = msg.createReply();
                    reply.setPerformative(ACLMessage.INFORM);
                    reply.setContent("\n" + response);
                    send(reply);
                } else {
                    block();
                }
            }
        });
    }
}