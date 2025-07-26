package com.github.freshmorsikov.node

import ai.koog.agents.core.dsl.builder.AIAgentNodeDelegate
import ai.koog.agents.core.dsl.builder.AIAgentSubgraphBuilderBase

fun AIAgentSubgraphBuilderBase<*, *>.nodeSayToUser(): AIAgentNodeDelegate<String, Unit> =
    node("sayToUser") { input ->
        println(input)
    }