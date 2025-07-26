package com.github.freshmorsikov

import ai.koog.agents.core.agent.AIAgent
import ai.koog.agents.core.dsl.builder.AIAgentNodeDelegate
import ai.koog.agents.core.dsl.builder.AIAgentSubgraphBuilderBase
import ai.koog.agents.core.dsl.builder.forwardTo
import ai.koog.agents.core.dsl.builder.strategy
import ai.koog.agents.core.dsl.extension.nodeLLMRequest
import ai.koog.agents.core.tools.ToolRegistry
import ai.koog.agents.features.eventHandler.feature.EventHandler
import ai.koog.agents.features.eventHandler.feature.EventHandlerConfig
import ai.koog.prompt.executor.clients.openai.OpenAIModels
import ai.koog.prompt.executor.llms.all.simpleOpenAIExecutor
import ai.koog.prompt.message.Message
import kotlinx.coroutines.runBlocking

fun main(): Unit = runBlocking {
    val apiKey = System.getenv("OPENAI_API_KEY")
    val eventHandlerConfig: EventHandlerConfig.() -> Unit = {}
    val toolRegistry = ToolRegistry {}
    val loopingStrategy = strategy(name = "looping") {
        val nodeAskUser by nodeAskUser()
        val nodeCallLLM by nodeLLMRequest(allowToolCalls = false)
        val nodeSayToUser by nodeSayToUser()

        edge(nodeStart forwardTo nodeCallLLM)
        edge(nodeCallLLM forwardTo nodeSayToUser)
        edge(nodeSayToUser forwardTo nodeAskUser)
        edge(nodeAskUser forwardTo nodeCallLLM onCondition { !it.contains("stop") })
        edge(nodeAskUser forwardTo nodeFinish onCondition { it == "stop".trim() })
    }
    val agent = AIAgent(
        executor = simpleOpenAIExecutor(apiKey),
        llmModel = OpenAIModels.Chat.GPT4o,
        strategy = loopingStrategy,
        systemPrompt = """
                You are an expert tutor. Whenever the user asks about a topic:
    
                1. Give a brief overview of the topic.
                2. Make a high-level plan for studying this topic. List the points of this plan numbered from 1 to n.
                3. When the user replies with a number:
                   – If 1–n: a brief overview of that subtopic, then again make a high-level plan for studying this subtopic. List the points of this plan numbered from 1 to n.
                   - Else: suggest to select subtopic of enter his oun subtopic.
                4. Continue until the user is satisfied.
                
                Always follow this format exactly, and don’t add extra commentary.
            """,
        toolRegistry = toolRegistry,
        installFeatures = {
            install(
                feature = EventHandler,
                configure = eventHandlerConfig,
            )
        }
    )

    println("Hello, I'm your tutor. Let's pick a topic first:")
    agent.run(agentInput = readln())
}

fun AIAgentSubgraphBuilderBase<*, *>.nodeSayToUser(): AIAgentNodeDelegate<Message.Response, Unit> =
    node("sayToUser") { input ->
        println(input.content)
    }

fun AIAgentSubgraphBuilderBase<*, *>.nodeAskUser(): AIAgentNodeDelegate<Unit, String> =
    node("askUser") {
        readln()
    }