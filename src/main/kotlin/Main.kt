package com.github.freshmorsikov

import ai.koog.agents.core.agent.AIAgent
import ai.koog.agents.core.dsl.builder.forwardTo
import ai.koog.agents.core.dsl.builder.strategy
import ai.koog.agents.core.dsl.extension.nodeLLMRequest
import ai.koog.agents.core.tools.ToolRegistry
import ai.koog.agents.ext.tool.AskUser
import ai.koog.agents.features.eventHandler.feature.EventHandler
import ai.koog.agents.features.eventHandler.feature.EventHandlerConfig
import ai.koog.prompt.executor.clients.openai.OpenAIModels
import ai.koog.prompt.executor.llms.all.simpleOpenAIExecutor
import com.github.freshmorsikov.node.nodeAskUser
import com.github.freshmorsikov.node.nodeGoBack
import com.github.freshmorsikov.node.nodeSayToUser
import kotlinx.coroutines.runBlocking

private const val BACK_COMMAND = "back"
private const val STOP_COMMAND = "stop"

fun main(): Unit = runBlocking {
    val apiKey = System.getenv("OPENAI_API_KEY")
    val eventHandlerConfig: EventHandlerConfig.() -> Unit = {}
    val toolRegistry = ToolRegistry {
        tool(AskUser)
    }
    val loopingStrategy = strategy(name = "Learning") {
        val nodeAskUser by nodeAskUser()
        val nodeCallLLM by nodeLLMRequest(allowToolCalls = false)
        val nodeSayToUser by nodeSayToUser()
        val nodeGoBack by nodeGoBack()

        edge(nodeStart forwardTo nodeCallLLM)
        edge(nodeCallLLM forwardTo nodeSayToUser transformed { it.content })
        edge(nodeSayToUser forwardTo nodeAskUser)
        edge(nodeAskUser forwardTo nodeCallLLM onCondition { shouldCallLLM(userMessage = it) })
        edge(nodeAskUser forwardTo nodeGoBack onCondition { shouldGoBack(userMessage = it) })
        edge(nodeGoBack forwardTo nodeSayToUser)
        edge(nodeAskUser forwardTo nodeFinish onCondition { shouldStop(userMessage = it) })
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

private fun shouldCallLLM(userMessage: String): Boolean {
    val messageNumbers = userMessage.filter { it.isDigit() }
    return messageNumbers.isNotEmpty()
}

private fun shouldGoBack(userMessage: String): Boolean {
    return userMessage.trim()
        .equals(
            other = BACK_COMMAND,
            ignoreCase = true
        )
}

private fun shouldStop(userMessage: String): Boolean {
    return userMessage.trim()
        .equals(
            other = STOP_COMMAND,
            ignoreCase = true
        )
}
