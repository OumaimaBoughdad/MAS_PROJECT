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
        p.setParameter(Profile.MAIN_PORT, "1098"); // Change to an available port
        p.setParameter(Profile.GUI, "true");
        AgentContainer container = rt.createMainContainer(p);

        try {
            // Create and start all supporting agents first
            AgentController internal = container.createNewAgent(
                    "InternalAgent", "agents.InternalAgent", null);
            internal.start();

            // Start user agent with GUI last
            AgentController user = container.createNewAgent(
                    "UserAgent", "agents.UserAgent", null);
            user.start();

            AgentController execution = container.createNewAgent(
                    "ExecutionAgent", "agents.ExecutionAgent", null);
            execution.start();

            // Create and start all resource agents
            AgentController wiki = container.createNewAgent(
                    "WikipediaAgent", "agents.WikipediaAgent", null);
            wiki.start();

            AgentController ddg = container.createNewAgent(
                    "DuckDuckGoAgent", "agents.DuckDuckGoAgent", null);
            ddg.start();

            AgentController widata = container.createNewAgent(
                    "WikidataAgent", "agents.WikidataAgent", null);
            widata.start();

            AgentController book = container.createNewAgent(
                    "BookSearchAgent", "agents.BookSearchAgent", null);
            book.start();

            AgentController lsg = container.createNewAgent(
                    "LangSearchAgent", "agents.LangSearchAgent", null);
            lsg.start();

            // Add the new OpenRouter agent
            AgentController openRouter = container.createNewAgent(
                    "OpenRouterAgent", "agents.OpenRouterAgent", null);
            openRouter.start();

            // Add Together AI agent
            AgentController together = container.createNewAgent(
                    "TogetherAgent", "agents.TogetherAgent", null);
            together.start();

            //DeepInfraAgent

            // In your main container setup:
            AgentController deepInfraAgent = container.createNewAgent(
                    "DeepInfraAgent",
                    "agents.DeepInfraAgent",
                    new Object[]{}
            );
            deepInfraAgent.start(); // Make sure this is called

            // Start wolfmath agent
            AgentController wolfram = container.createNewAgent(
                    "WolframAlphaAgent", "agents.WolframAlphaAgent", null);
            wolfram.start();

            // Start the broker agent (needs to be after resource agents)
            AgentController broker = container.createNewAgent(
                    "BrokerAgent", "agents.BrokerAgent", null);
            broker.start();

            System.out.println("All agents started successfully!");

        } catch (StaleProxyException e) {
            System.err.println("Error starting agents:");
            e.printStackTrace();
        }
    }
}