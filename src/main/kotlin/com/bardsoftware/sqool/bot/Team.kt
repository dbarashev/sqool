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

import com.bardsoftware.libbotanique.*
import com.bardsoftware.sqool.bot.db.tables.records.TeampeerassessmentstatusRecord
import com.bardsoftware.sqool.bot.db.tables.references.*
import org.jooq.impl.DSL.field
import org.jooq.impl.DSL.table
import java.math.BigDecimal
import java.util.*
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.launch
import java.util.concurrent.Executors

//import org.jooq.impl.DSL.*
data class Student(val tgUsername: String, val name: String, val tgUserid: BigDecimal, val id: Int) {
  fun updateTgUserId(userId: Long) {
    db {
      update(table("Student")).set(field("tg_userid", BigDecimal::class.java), userId.toBigDecimal())
        .where(field("tg_username", String::class.java).eq(tgUsername))
        .execute()
    }
  }
}

fun getStudent(tgUsername: String): Student? {
  return db {
    selectFrom(STUDENT).where(STUDENT.TG_USERNAME.eq(tgUsername)).fetchOne()?.let {
      Student(it.tgUsername!!, it.name!!, it.tgUserid!!.toBigDecimal(), it.id!!)
    }
  }
}

fun getStudent(studentId: Int): Student? {
  return db {
    selectFrom(STUDENT).where(STUDENT.ID.eq(studentId)).fetchOne()?.let {
      Student(it.tgUsername!!, it.name!!, it.tgUserid!!.toBigDecimal(), it.id!!)
    }
  }
}
fun insertStudent(tgUsername: String, name: String, tgUserId: Long) {
  db {
    insertInto(table("Student"), field("tg_username"), field("name"), field("tg_userid"))
      .values(tgUsername, name, tgUserId.toBigDecimal())
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

fun getTeammates(tgUsername: String, sprintNum: Int): Result<Teammates> {
  return db {
    val studentRecord =
        select(field("team_num", Int::class.java), field("ord", Int::class.java))
            .from(table("Team"))
            .where(field("sprint_num").eq(sprintNum)
                .and(field("tg_username").eq(tgUsername)))
            .fetchOne() ?: return@db Result.failure(RuntimeException("Can't find team record fpr $tgUsername@sprint $sprintNum"))

    val mates =
        select(
            TEAMDETAILS.NAME, TEAMDETAILS.TG_USERNAME, TEAMDETAILS.ORD, TEAMDETAILS.ID, TEAMDETAILS.GH_USERNAME
        ).from(TEAMDETAILS.join(TEAM).using(field("team_num")))
            .where(TEAM.SPRINT_NUM.eq(sprintNum)
            .and(TEAMDETAILS.SPRINT_NUM.eq(sprintNum))
            .and(TEAM.TG_USERNAME.eq(tgUsername))
        ).map {row ->
          TeamMember(
            teamNum = studentRecord.component1(),
            tgUsername = row[TEAMDETAILS.TG_USERNAME]!!,
            ord = row[TEAMDETAILS.ORD]!!,
            displayName = row[TEAMDETAILS.NAME]!!,
            id = row[TEAMDETAILS.ID]!!,
            githubUsername = row[TEAMDETAILS.GH_USERNAME]!!
          )
        }
    Result.success(Teammates(sprintNum, studentRecord.component1(), mates))
  }
}

fun getCurrentTeammates(tgUsername: String) = getTeammates(tgUsername, 0)
data class Teammates(
    val sprintNum: Int, val teamNum: Int,
    val members: List<TeamMember>)

fun getPrevTeammates(tgUsername: String): Result<Teammates> {
  val allSprints = getAllSprints(tgUsername)
  val maxSprint = allSprints.maxOrNull() ?: return Result.failure(RuntimeException("No team on the last sprint"))
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

data class TeamMember(
  val teamNum: Int,
  val tgUsername: String,
  val ord: Int,
  val githubUsername: String = "",
  val displayName: String = "",
  val id: Int = -1,
  val relocatedFrom: Int = -1) {
    val isActive = id != -42
    fun isNewTeamOk(newTeamNum: Int)= if (relocatedFrom != -1) relocatedFrom != newTeamNum else teamNum != newTeamNum
}
data class Team(val teamNum: Int, val members: MutableList<TeamMember>) {
    fun needsMember(ord: Int) = (members.size > ord && !members[ord].isActive) || (members.size <= ord)
    fun hasActive(ord: Int) = members.size > ord && members[ord].isActive
    val isActive: Boolean get() = members.all { it.isActive }
}

fun initialTeams(): List<TeamMember> {
  val initialRecords = db {
    select().from(STUDENT).where(STUDENT.IS_ACTIVE.isTrue).toList()
  }.shuffled().mapIndexed { idx, row ->
    TeamMember(
      teamNum = idx / 2 + 1,
      tgUsername = row[STUDENT.TG_USERNAME]!!,
      ord =idx % 2 + 1,
      githubUsername = row[STUDENT.GH_USERNAME]!!,
      displayName = row[STUDENT.NAME]!!
    )
  }.toList()
  return initialRecords
}

fun rotateTeams(actualMembers: List<TeamMember>): List<TeamMember> {
//    select(
//        field("team_num", Int::class.java),
//        field("tg_username", String::class.java),
//        field("ord", Int::class.java)
//    ).from(table("Team")).where(field("sprint_num").eq(lastSprint)
//        .and(field("team_num", Int::class.java).between(uni * 100, (uni + 1) * 100)))
//        .orderBy(field("team_num", Int::class.java), field("ord", Int::class.java))
//        .map { TeamMember(it.value1(), it.value2(), it.value3()) }.toList()

  val activeStatuses = db {
      select(field("tg_username", String::class.java), field("is_active", Boolean::class.java))
          .from(table("student"))
          .map { it.value1() to it.value2() }
  }
  val actualTeams = buildActualTeams(actualMembers, activeStatuses)
  val packedActualTeams = packTeams(actualTeams)

  val teamNums = packedActualTeams.map { it.teamNum }.sorted()
  println("Cur team nums=$teamNums")

  // Генерируем новую перестанову номеров команд, пока не получим "хорошую".
  // Хорошая перестановка -- это такая где одни и те же номера не стоят на одной и той же позиции.
  // Это нужно, чтобы сеньоры в новой ротации попали на новые проекты.
  val newTeamNums = teamNums.toMutableList()
  var doShuffle = true
  while (doShuffle) {
    newTeamNums.shuffle()
    doShuffle = !actualTeams.zip(newTeamNums).all { it.first.members[0].isNewTeamOk(it.second) }
  }
  println("New team nums=$newTeamNums")

    return rotateTeamMembers2(packedActualTeams.flatMap { it.members }, teamNums, newTeamNums)
}

fun rotateTeamMembers2(members: List<TeamMember>, teamNums: List<Int>, newTeamNums: List<Int>): List<TeamMember> {
    val losers = members.filter { it.ord == 3 }.associate { it.teamNum to 2 }
    val newRecords: List<TeamMember> = members.map {
        val teamIdx = teamNums.indexOf(it.teamNum)
        when (it.ord) {
            1 -> TeamMember(
              teamNum = newTeamNums[teamIdx], tgUsername = it.tgUsername, ord = 2, displayName = it.displayName, id = it.id,
              githubUsername = it.githubUsername
            )
            2 ->
                // Если ты был вторым в команде из трех, то будешь третьим в другой команде, а иначе становишься первым
                if (losers.containsKey(it.teamNum)) {
                    TeamMember(
                      teamNum = randomTeam(newTeamNums, listOf(it.teamNum)), tgUsername = it.tgUsername, ord = 3, displayName = it.displayName, id = it.id,
                      githubUsername = it.githubUsername
                    )
                } else {
                    TeamMember(
                      teamNum = it.teamNum, tgUsername = it.tgUsername, ord = 1, displayName = it.displayName, id = it.id,
                      githubUsername = it.githubUsername
                    )
                }
            3 -> TeamMember(
              teamNum = it.teamNum, tgUsername = it.tgUsername, ord = 1, displayName = it.displayName, id = it.id,
              githubUsername = it.githubUsername
            )
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
fun buildActualTeams(members: List<TeamMember>, activeStatuses: List<Pair<String, Boolean>>): List<Team> {
    val activeMembers = activeStatuses.filter { it.second }.map { it.first }.toSet()
    val result = mutableListOf<Team>()
  println("active students: $activeMembers")
    members.groupBy { it.teamNum }.forEach { (teamNum, members) ->
        val orderedMembers = members.sortedBy { it.ord }.map {
            if (activeMembers.contains(it.tgUsername)) it
            else TeamMember(teamNum, it.tgUsername, it.ord, displayName = it.displayName, id = -42, githubUsername = it.githubUsername)
        }.toMutableList()
        result.add(Team(teamNum, orderedMembers))
    }
    return result
}

fun packTeams(teams: List<Team>): List<Team> {
    val result = mutableListOf<Team>()
    val needsMember0 = LinkedList<Team>()
    val needsMember1 = LinkedList<Team>()

    teams.forEach {team ->
        println("Team ${team.teamNum}:\n$team")
        when {
          team.needsMember(0) -> needsMember0.add(team)
          team.needsMember(1) -> needsMember1.add(team)
          team.isActive -> result.add(team)
        }
    }

    println("Teams with member0 dropped: $needsMember0")
    println("Teams with member1 dropped: $needsMember1")
    println("Active teams count=${result.size}")
    val stillNeedsMember0 = LinkedList<Team>()
    while (needsMember0.isNotEmpty()) {
        val team = needsMember0.poll()
        needsMember1.poll()?.also {
            team.members[0] = TeamMember(
              teamNum = team.teamNum,
              tgUsername = it.members[0].tgUsername,
              ord = 1,
              displayName = it.members[0].displayName,
              id = it.members[0].id,
              relocatedFrom = it.teamNum,
              githubUsername = it.members[0].githubUsername
            )
            result.add(team)
        } ?: run {
            stillNeedsMember0.add(team)
        }
    }

    println("These teams need member 0: $stillNeedsMember0")
    println("These teams need member 1: $needsMember1")
    return result
}

fun finishIteration(uni: Int): Int? =
    txn {
      val lastSprint = lastSprint(uni) ?: return@txn -1
      update(TEAM).set(TEAM.SPRINT_NUM, lastSprint + 1)
        .where(TEAM.TEAM_NUM.lessThan((uni+1)*100)).and(TEAM.SPRINT_NUM.eq(0))
        .execute()
      lastSprint + 1
    }

private fun randomTeam(teams: List<Int>, exceptions: List<Int>) = teams.toMutableList().subtract(exceptions).random()

fun insertNewRotation(records: List<TeamMember>) {
  txn {
    records.forEach {
      insertInto(
        TEAM,
          TEAM.SPRINT_NUM,
          TEAM.TEAM_NUM,
          TEAM.TG_USERNAME,
          TEAM.ORD
      ).values(0, it.teamNum, it.tgUsername, it.ord)
        .onConflict().doUpdate()
        .set(TEAM.TEAM_NUM, it.teamNum).set(TEAM.ORD, it.ord)

    }
  }
}

fun updateRepositoryPermissions(currentTeamMembers: List<TeamMember>, pastTeamMembers: List<TeamMember>) {
  val client = HttpClient(CIO)
  val baseUrl = """https://api.github.com/repos/${System.getenv("GIT_ORG")}"""
  backgroundTasks.launch {
    pastTeamMembers.forEach {
      val url = "$baseUrl/project${it.teamNum}/collaborators/${it.githubUsername}"
      val status = client.delete(url) {
        headers {
          append(HttpHeaders.Accept, "application/vnd.github+json")
          append(HttpHeaders.Authorization, "Bearer ${System.getenv("GIT_TOKEN")}")
          append("X-GitHub-Api-Version", "2022-11-28")
        }
      }.status
      println("Student ${it.githubUsername} removed from repo project${it.teamNum}, status=$status, url=$url")
    }
    currentTeamMembers.filter { it.isActive }.forEach {
      val url = "$baseUrl/project${it.teamNum}/collaborators/${it.githubUsername}"
      val status = client.put(url) {
        headers {
          append(HttpHeaders.Accept, "application/vnd.github+json")
          append(HttpHeaders.Authorization, "Bearer ${System.getenv("GIT_TOKEN")}")
          append("X-GitHub-Api-Version", "2022-11-28")
        }
        setBody("""{"permission":"write"}""")
      }.status
      println("Student ${it.githubUsername} was granted access to repository project${it.teamNum}, status=$status, url=$url")
    }
  }
}
fun getAllCurrentTeamRecords(uni: Int): List<TeamMember> = getAllSprintTeamRecords(uni, 0)

fun getAllSprintTeamRecords(uni: Int, sprintNum: Int): List<TeamMember> =
    db {
        // select team_num, name from Team T JOIN Student S USING (tg_username) WHERE sprint_num = 0 ORDER BY team_num, ord
        select(TEAM.TEAM_NUM, STUDENT.NAME, STUDENT.ID, STUDENT.GH_USERNAME, STUDENT.TG_USERNAME, TEAM.ORD)
            .from(TEAM.join(STUDENT).using(field("tg_username")))
            .where(
              TEAM.SPRINT_NUM.eq(sprintNum)
            ).and(
              TEAM.TEAM_NUM.between(uni * 100, (uni + 1) * 100)
            )
            .orderBy(TEAM.TEAM_NUM, TEAM.ORD)
            .map { TeamMember(
              teamNum = it[TEAM.TEAM_NUM]!!,
              tgUsername = it[STUDENT.TG_USERNAME]!!,
              ord = it[TEAM.ORD]!!,
              displayName = it[STUDENT.NAME]!!,
              id = it[STUDENT.ID]!!,
              githubUsername = it[STUDENT.GH_USERNAME]!!
            ) }
    }

fun findMissingPeerAssessments(uni: Int): List<TeampeerassessmentstatusRecord> =
  db {
    selectFrom(TEAMPEERASSESSMENTSTATUS).where(TEAMPEERASSESSMENTSTATUS.IS_DONE.isFalse).and(TEAMPEERASSESSMENTSTATUS.SPRINT_NUM.eq(
      lastSprint(uni)
    )).toList()
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

fun List<TeamMember>.print(tg: ChainBuilder) {
  var teamNum = -1
  val buf = StringBuffer()
  this.forEach {
    if (it.teamNum > teamNum) {
      buf.append("\n\n")
      teamNum = it.teamNum
      buf.append("__team ${teamNum}__\n")
    }
    buf.append(it.displayName.escapeMarkdown()).append("\n")
  }
  tg.reply(buf.toString(), isMarkdown = true)
}

private val backgroundTasks = CoroutineScope(Executors.newFixedThreadPool(3).asCoroutineDispatcher())