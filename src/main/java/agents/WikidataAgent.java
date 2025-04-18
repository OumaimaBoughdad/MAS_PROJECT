package agents;

import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import utils.HttpHelper;

public class WikidataAgent extends Agent {
    protected void setup() {
        System.out.println(getAID().getName() + " is ready for Wikidata searches.");

        addBehaviour(new CyclicBehaviour(this) {
            public void action() {
                ACLMessage msg = receive(MessageTemplate.MatchPerformative(ACLMessage.REQUEST));
                if (msg != null) {
                    String query = msg.getContent();
                    System.out.println("Searching Wikidata for: " + query);
                    String response = HttpHelper.searchExternalSource("wikidata", query);

                    ACLMessage reply = msg.createReply();
                    reply.setPerformative(ACLMessage.INFORM);
                    // Prefix exactly like your other agents do:
                    reply.setContent("\n" + response);
                    send(reply);
                } else {
                    block();
                }
            }
        });
    }
}
