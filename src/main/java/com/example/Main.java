package com.example;

import jade.core.Profile;
import jade.core.ProfileImpl;
import jade.core.Runtime;
import jade.wrapper.AgentContainer;
import jade.wrapper.AgentController;
import jade.wrapper.StaleProxyException;

public class Main {
    public static void main(String[] args) {
        // Start JADE platform
        Runtime rt = Runtime.instance();
        Profile p = new ProfileImpl();
        p.setParameter(Profile.MAIN_HOST, "localhost");
        p.setParameter(Profile.GUI, "true");
        AgentContainer container = rt.createMainContainer(p);

        try {
            // Create and start agents
            AgentController broker = container.createNewAgent(
                    "BrokerAgent", "agents.BrokerAgent", null);
            broker.start();

            AgentController internal = container.createNewAgent(
                    "InternalAgent", "agents.InternalAgent", null);
            internal.start();

            AgentController execution = container.createNewAgent(
                    "ExecutionAgent", "agents.ExecutionAgent", null);
            execution.start();

            AgentController wiki = container.createNewAgent(
                    "WikipediaAgent", "agents.WikipediaAgent", null);
            wiki.start();

            AgentController ddg = container.createNewAgent(
                    "DuckDuckGoAgent", "agents.DuckDuckGoAgent", null);
            ddg.start();

            AgentController book = container.createNewAgent(
                    "BookSearchAgent", "agents.BookSearchAgent", null);
            book.start();

            // Start user agent last
            AgentController user = container.createNewAgent(
                    "UserAgent", "agents.UserAgent", null);
            user.start();
        } catch (StaleProxyException e) {
            e.printStackTrace();
        }
    }
}