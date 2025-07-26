package com.github.freshmorsikov.node

import ai.koog.agents.core.dsl.builder.AIAgentNodeDelegate
import ai.koog.agents.core.dsl.builder.AIAgentSubgraphBuilderBase
import ai.koog.prompt.message.Message.Role

fun AIAgentSubgraphBuilderBase<*, *>.nodeGoBack(): AIAgentNodeDelegate<String, String> =
    node("goBack") {
        if (llm.prompt.messages.count { message -> message.role == Role.Assistant } < 2) {
            return@node "Not enough messages"
        }

        llm.writeSession {
            rewritePrompt {
                var lastAssistantMessageRemoved = false

                prompt.copy(
                    messages = prompt.messages.dropLastWhile { message ->
                        val predicate = !lastAssistantMessageRemoved || message.role != Role.Assistant
                        if (message.role == Role.Assistant) {
                            lastAssistantMessageRemoved = true
                        }

                        predicate
                    }
                )
            }
        }

        llm.prompt.messages.last().content
    }