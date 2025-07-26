package com.github.freshmorsikov.node

import ai.koog.agents.core.dsl.builder.AIAgentNodeDelegate
import ai.koog.agents.core.dsl.builder.AIAgentSubgraphBuilderBase

fun AIAgentSubgraphBuilderBase<*, *>.nodeAskUser(): AIAgentNodeDelegate<Unit, String> =
    node("askUser") {
        readln()
    }