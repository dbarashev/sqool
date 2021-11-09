/*
 * Copyright (c) BarD Software s.r.o 2021
 *
 * This file is a part of SQooL, a service for running SQL contests.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bardsoftware.sqool.bot

import org.jooq.impl.DSL.field
import org.jooq.impl.DSL.table

//import org.jooq.impl.DSL.*

fun getAllSprints(tgUsername: String): List<Int> {
  return db {
    select(field("sprint_num", Int::class.java)).from(table("Team"))
        .where(field("tg_username").eq(tgUsername))
        .map { it.value1() }
  }
}

fun getTeammates(tgUsername: String, sprintNum: Int): List<Pair<String, String>> {
  return db {
    select(field("Student.name", String::class.java), field("Student.tg_username", String::class.java))
        .from(table("Student")
            .join(table("Team").`as`("t2")).using(field("tg_username"))
            .join(table("Team").`as`("t1")).using(field("team_num")))
        .where(field("t1.sprint_num").eq(sprintNum)
            .and(field("t2.sprint_num").eq(sprintNum))
            .and(field("t1.tg_username").eq(tgUsername))
        ).map {
          it.value1() to it.value2()
        }
  }
}

fun getCurrentTeammates(tgUsername: String): List<Pair<String, String>> = getTeammates(tgUsername, 0)
data class Teammates(val sprintNum: Int, val members: List<Pair<String, String>>)

fun getPrevTeammates(tgUsername: String): Teammates {
  val allSprints = getAllSprints(tgUsername)
  val maxSprint = allSprints.maxOrNull() ?: return Teammates(-1, listOf())
  return Teammates(maxSprint, getTeammates(tgUsername, maxSprint))
}

fun setScore(tgUsernameFrom: String, tgUsernameTo: String, score: Double, sprintNum: Int) {
  txn {
    insertInto(table("Score"), field("tg_username_from"), field("tg_username_to"), field("sprint_num"), field("score"))
        .values(tgUsernameFrom, tgUsernameTo, sprintNum, score)
        .onConflict(field("tg_username_from"), field("tg_username_to"), field("sprint_num")).doUpdate().set(field("score"), score)
        .execute()
  }
}

data class ScoreRecord(val teamNum: Int, val name: String, val scoreSumFormula: String, val scoreSources: Map<String, Double>)

fun getAllScores(uni: Int): List<ScoreRecord> {
  return db {
    val lastSprint = select(field("sprint_num", Int::class.java))
        .from(table("LastSprint"))
        .where(field("uni").eq(uni)).fetchOne()?.value1()
    val records = select(
        field("sprint_num", Int::class.java),
        field("team_num", Int::class.java),
        field("ord_to", Int::class.java),
        field("tg_username_to", String::class.java),
        field("score", Double::class.java),
        field("tg_username_from", String::class.java),
        field("ord_from", Int::class.java)
    ).from(table("PeerScores")).where(field("sprint_num").eq(lastSprint))
        .orderBy(field("sprint_num"), field("team_num"), field("ord_to"), field("ord_from")).toList()
    var currentTeam = 0
    var currentOrdTo = 0
    var currentTgUsername = ""
    val scores = mutableListOf<Pair<String, Double>>()
    val result = mutableListOf<ScoreRecord>()
    for (rec in records) {
      if (rec.value2() > currentTeam) {
        if (scores.isNotEmpty()) {
          result.add(ScoreRecord(currentTeam, currentTgUsername,
              "=" + scores.map { it.second }.joinToString("+").ifEmpty { "0" },
              scores.associate { it }
          ))
        }
        currentTeam = rec.value2()
        currentOrdTo = rec.value3()
        currentTgUsername = rec.value4()
        scores.clear()
        val ordFrom = rec.value7()
        if (ordFrom != null) {
          if (currentOrdTo < 3 && ordFrom >=3 || currentOrdTo >= 3 && ordFrom < 3) {
            scores.add(rec.value6() to rec.value5())
          }
        }
      } else {
        if (rec.value3() > currentOrdTo) {
          if (scores.isNotEmpty()) {
            println(scores)
            result.add(ScoreRecord(currentTeam, currentTgUsername,
                "=" + scores.map { it.second }.joinToString("+").ifEmpty { "0" },
                scores.associate { it }
            ))
          }
          currentOrdTo = rec.value3()
          currentTgUsername = rec.value4()
          scores.clear()
          val ordFrom = rec.value7()
          if (ordFrom != null) {
            if (currentOrdTo < 3 && ordFrom >=3 || currentOrdTo >= 3 && ordFrom < 3) {
              scores.add(rec.value6() to rec.value5())
            }
          }
        } else {
          val ordFrom = rec.value7()
          if (ordFrom != null) {
            if (currentOrdTo < 3 && ordFrom >=3 || currentOrdTo >= 3 && ordFrom < 3) {
              scores.add(rec.value6() to rec.value5())
            }
          }
        }
      }
    }
    if (scores.isNotEmpty()) {
      result.add(ScoreRecord(currentTeam, currentTgUsername,
          "=" + scores.map { it.second }.joinToString("+").ifEmpty { "0" },
          scores.associate { it }
      ))
    }
    result
  }
}
