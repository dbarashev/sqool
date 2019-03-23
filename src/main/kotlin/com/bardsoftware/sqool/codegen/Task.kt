package com.bardsoftware.sqool.codegen

sealed class Task(val name: String, val robotQuery: String)

class ScalarValueTask(name: String,
                      robotQuery: String,
                      val resultType: SqlDataType
) : Task(name, robotQuery)

class SingleColumnTask(name: String,
                       robotQuery: String,
                       val spec: TaskResultColumn
) : Task(name, robotQuery)

class MultiColumnTask(name: String,
                      robotQuery: String,
                      val matcherSpec: MatcherSpec
) : Task(name, robotQuery)