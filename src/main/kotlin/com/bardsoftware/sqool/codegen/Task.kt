package com.bardsoftware.sqool.codegen

sealed class Task(val name: String, val robotQuery: String) {
    abstract val resultType: String
}

class ScalarValueTask(name: String,
                      robotQuery: String,
                      private val _resultType: SqlDataType
) : Task(name, robotQuery) {
    override val resultType: String
        get() = _resultType.toString()
}

class SingleColumnTask(name: String,
                       robotQuery: String,
                       val spec: TaskResultColumn
) : Task(name, robotQuery) {
    override val resultType: String
        get() = "TABLE($spec)"
}

class MultiColumnTask(name: String,
                      robotQuery: String,
                      val matcherSpec: MatcherSpec
) : Task(name, robotQuery) {
    override val resultType: String
        get() = matcherSpec.relationSpec.getAllColsList().joinToString(", ", "TABLE(", ")")
}