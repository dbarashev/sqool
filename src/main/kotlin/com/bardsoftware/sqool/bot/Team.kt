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
import kotlin.random.Random

//import org.jooq.impl.DSL.*
data class Student(val tgUsername: String, val name: String)
fun getStudent(tgUsername: String): Student? {
  return db {
    select(field("name", String::class.java))
        .from(table("Student"))
        .where(field("tg_username").eq(tgUsername))
        .fetchOne()?.let { Student(tgUsername, it.value1()) }
  }
}

fun insertStudent(tgUsername: String, name: String) {
  db {
    insertInto(table("Student"), field("tg_username"), field("name")).values(tgUsername, name)
        .onConflict(field("tg_username")).doUpdate().set(field("name"), name)
        .execute()
  }
}

fun getAllSprints(tgUsername: String): List<Int> {
  return db {
    select(field("sprint_num", Int::class.java)).from(table("Team"))
        .where(field("tg_username").eq(tgUsername))
        .map { it.value1() }
  }
}

fun getTeammates(tgUsername: String, sprintNum: Int): Teammates {
  return db {
    val studentRecord =
        select(field("team_num", Int::class.java), field("ord", Int::class.java))
            .from(table("Team"))
            .where(field("sprint_num").eq(sprintNum)
                .and(field("tg_username").eq(tgUsername)))
            .fetchOne() ?: throw RuntimeException("Can't find team record fpr $tgUsername@sprint $sprintNum")

    val mates =
        select(
            field("M.name", String::class.java),
            field("M.tg_username", String::class.java),
            field("M.ord", Int::class.java),
            field("M.id", Int::class.java)
        ).from(table("TeamDetails").`as`("m").join(table("Team").`as`("t1")).using(field("team_num")))
            .where(field("t1.sprint_num").eq(sprintNum)
            .and(field("M.sprint_num").eq(sprintNum))
            .and(field("t1.tg_username").eq(tgUsername))
        ).map {
          TeamRecord(studentRecord.component1(), it.component2(), it.component3(), it.component1(), it.component4())
        }
    Teammates(sprintNum, studentRecord.component1(), mates)
  }
}

fun getCurrentTeammates(tgUsername: String) = getTeammates(tgUsername, 0)
data class Teammates(
    val sprintNum: Int, val teamNum: Int,
    val members: List<TeamRecord>)

fun getPrevTeammates(tgUsername: String): Teammates {
  val allSprints = getAllSprints(tgUsername)
  val maxSprint = allSprints.maxOrNull() ?: return Teammates(-1, 0, listOf())
  return getTeammates(tgUsername, maxSprint)
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
    ).from(table("PeerScores"))
        .where(field("sprint_num").eq(lastSprint)).and(field("team_num", Int::class.java).between(uni * 100, (uni + 1) * 100))
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

data class TeamRecord(val teamNum: Int, val tgUsername: String, val ord: Int, val displayName: String = "", val id: Int = -1)

fun rotateTeams(uni: Int): List<TeamRecord> {
  // Получаем информацию о нынешних составах команд.
  val records = db {
    val lastSprint = select(field("sprint_num", Int::class.java))
        .from(table("LastSprint"))
        .where(field("uni").eq(uni)).fetchOne()?.value1()
    select(
        field("team_num", Int::class.java),
        field("tg_username", String::class.java),
        field("ord", Int::class.java)
    ).from(table("Team")).where(field("sprint_num").eq(lastSprint)
        .and(field("team_num", Int::class.java).between(uni * 100, (uni + 1) * 100)))
        .orderBy(field("team_num", Int::class.java), field("ord", Int::class.java))
        .map { TeamRecord(it.value1(), it.value2(), it.value3()) }.toList()
  }
  val teamNums = records.map { it.teamNum }.toSortedSet()
  println("Cur team nums=$teamNums")
  if (teamNums.isEmpty()) {
      val initialRecords = db {
        select(field("tg_username", String::class.java)).from(table("Student")).map { it.value1() }
      }.shuffled().mapIndexed { idx, tgUsername ->
          TeamRecord(idx / 2 + 1, tgUsername, idx % 2 + 1)
      }.toList()
      println(initialRecords)
      return initialRecords
  }

  // Генерируем новую перестанову номеров команд, пока не получим "хорошую".
  // Хорошая перестановка -- это такая где одни и те же номера не стоят на одной и той же позиции и не стоят
  // соседних позициях. Это нужно, чтобы сеньоры в новой ротации попали на новые проекты.
  val newTeamNums = teamNums.toMutableList()
  var doShuffle = true
  while (doShuffle) {
    newTeamNums.shuffle()
    val matchedPair = teamNums.zip(newTeamNums).firstOrNull { it.first == it.second }
    val matchedShiftedPair = newTeamNums.toMutableList().also {
      it.add(0, it.removeLast())
    }.zip(teamNums).firstOrNull { it.first == it.second }
    doShuffle = matchedPair != null || matchedShiftedPair != null
  }
  println("New team nums=$newTeamNums")

  // Ищем команды, где джунов было больше чем 2. В них один из джунов станет лузером и останется в новой итерации
  // джуном, переместившись в случайно выбранный проект под номером 5.
  // У этого джуна должен быть номер 3 или 4, потому что номер 5 случайно переместился в этот проект в прошлый раз и ждет повышения.
  //val randomLosers = records.filter { it.ord == 5 }.associate { it.teamNum to Random.nextInt(3, 5) }
    val randomLosers = records.filter { it.ord == 3 }.associate { it.teamNum to 2 }

  // Производим ротацию. Джуны (это те у кого ord > 2) остаются в нынешней команде, а сеньоры перемещаются в новые.
  // Сеньор 1 в команду, стоящую в массиве новой перестановки на той же позиции, что и сейчас.
  // Сеньор 2 в команду, стоящую в закольцованном массиве новой перестановки на предыдущей позиции.
  val newRecords: List<TeamRecord> = records.map {
    val teamIdx = teamNums.indexOf(it.teamNum)
    when (it.ord) {
      1 -> TeamRecord(teamNum = newTeamNums[teamIdx], tgUsername = it.tgUsername, ord = 2)
//      2 -> TeamRecord(teamNum = if (teamIdx == 0) newTeamNums.last() else newTeamNums[teamIdx-1],
//          tgUsername = it.tgUsername, ord = 4)
//      3 ->
//        // Если ты лузер, тебе ищут новую команду, а иначе идешь на повышение
//        if (it.ord == randomLosers[it.teamNum]) {
//          TeamRecord(teamNum = randomTeam(newTeamNums, listOf(it.teamNum)), tgUsername = it.tgUsername, ord = 5)
//        } else {
//          TeamRecord(teamNum = it.teamNum, tgUsername = it.tgUsername, ord = 2)
//        }
      2 ->
        // Если ты лузер, тебе ищут новую команду, а иначе идешь на повышение
        if (it.ord == randomLosers[it.teamNum]) {
          TeamRecord(teamNum = randomTeam(newTeamNums, listOf(it.teamNum)), tgUsername = it.tgUsername, ord = 3)
        } else {
          TeamRecord(teamNum = it.teamNum, tgUsername = it.tgUsername, ord = 1)
        }
//      5 -> randomLosers[it.teamNum]!!.let { loser ->
//        // Ты уже был лузером, поэтому сейчас лузер кто-то из твоих товарищей. Нужно выяснить, кто именно, чтобы
//        // стать сеньором вместо него.
//        TeamRecord(teamNum = it.teamNum, tgUsername = it.tgUsername, ord = if (loser == 3) 2 else 1)
//      }
        3 -> TeamRecord(teamNum = it.teamNum, tgUsername = it.tgUsername, ord = 1)
      else -> {
        println("unexpected ordinal number: ${it}")
        it
      }
    }
  }
  return newRecords.sortedWith { left, right ->
    val byTeam = left.teamNum - right.teamNum
    if (byTeam != 0) byTeam else left.ord - right.ord
  }
}

fun finishIteration(uni: Int): Int? =
    txn {
        val lastSprint = select(field("sprint_num", Int::class.java))
            .from(table("LastSprint"))
            .where(field("uni").eq(uni)).fetchOne()?.value1() ?: return@txn -1
        update(table("Team")).set(field("sprint_num", Int::class.java), lastSprint + 1)
            .where(field("team_num").lessThan((uni+1)*100)).and(field("sprint_num").eq(0)).execute()
        lastSprint + 1
    }

private fun randomTeam(teams: List<Int>, exceptions: List<Int>) = teams.toMutableList().subtract(exceptions).random()

fun insertNewRotation(records: List<TeamRecord>) {
  txn {
    records.forEach {
      insertInto(table("team"),
          field("sprint_num", Int::class.java),
          field("team_num", Int::class.java),
          field("tg_username", String::class.java),
          field("ord", Int::class.java)
      ).values(0, it.teamNum, it.tgUsername, it.ord).execute()
    }
  }
}

fun getAllCurrentTeamRecords(uni: Int): List<TeamRecord> = getAllSprintTeamRecords(uni, 0)

fun getAllSprintTeamRecords(uni: Int, sprintNum: Int): List<TeamRecord> =
    db {
        // select team_num, name from Team T JOIN Student S USING (tg_username) WHERE sprint_num = 0 ORDER BY team_num, ord
        select(field("team_num", Int::class.java), field("name", String::class.java), field("id", Int::class.java))
            .from(table("team").join(table("student")).using(field("tg_username")))
            .where(field("sprint_num").eq(sprintNum)).and(field("team_num", Int::class.java).between(uni * 100, (uni + 1) * 100))
            .orderBy(field("team_num"), field("ord"))
            .map { TeamRecord(it.component1(), "", 0, it.component2(), it.component3()) }
    }

fun lastSprint(uni: Int) =  db {
    select(field("sprint_num", Int::class.java))
        .from(table("LastSprint"))
        .where(field("uni").eq(uni)).fetchOne()?.value1()
}

fun sprintNumbers(uni: Int) = db {
    select(field("sprint_num", Int::class.java))
        .from(table("team"))
        .where(
            field("team_num", Int::class.java).between(uni*100, (uni+1)*100)
                .and(field("sprint_num", Int::class.java).ne(0))
        )
        .toSortedSet()
}