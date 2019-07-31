package com.bardsoftware.sqool.contest.storage

import com.bardsoftware.sqool.contest.admin.Contests
import com.bardsoftware.sqool.grader.AssessmentPubSubResp
import com.fasterxml.jackson.databind.ObjectMapper
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import org.joda.time.DateTime
import java.math.BigDecimal
import java.sql.PreparedStatement
import java.sql.Types
import java.util.Random

data class ChallengeOffer(val taskId: Int, val description: String)
data class UserEntity(var id: Int, var nick: String, var  name: String, var passwd: String)
data class TaskAttemptEntity(
    var taskEntity: TaskEntity,
    var userId: Int,
    var count: Int,
    var attemptId: String?,
    var status: String?,
    var testingStartMs: DateTime?,
    var errorMsg: String?,
    var resultSet: String?
)
data class TaskEntity(
    var id: Int,
    var name: String,
    var signature: String?,
    var description: String?,
    var score: Int,
    var difficulty: Int,
    var authorName: String
)

data class AuthorChallengeEntity(var authorId : Int, var authorName : String, var easyCount: Int, var mediumCount: Int, var difficultCount: Int, var gainTotal: BigDecimal)

object UserTable : Table("Contest.ContestUser") {
  var id = integer("id")
  var nick = text("nick")
  var name = text("name")
  var passwd = text("passwd")
}

object TaskByAuthorView : Table("Contest.TasksByAuthor") {
  var authorId = integer("id")
  var authorName = text("nick")
  var count1 = integer("count1")
  var gain1 = decimal("gain1", 4, 2)
  var count2 = integer("count2")
  var gain2 = decimal("gain2", 4, 2)
  var count3 = integer("count3")
  var gain3 = decimal("gain3", 4, 2)
  var gainTotal = decimal("total_gain", 4, 2)
}

object TaskTable : Table("Contest.Task") {
  var id = integer("id")
  var name = text("name")
  var signature = text("signature")
  var description = text("description")
  var score = integer("score")
  var difficulty = integer("difficulty")
  var authorId = integer("author_id")
}

object AttemptView : Table("Contest.MyAttempts") {
  var taskId = integer("task_id")
  var name = text("name")
  var description = text("description")
  var difficulty = integer("difficulty")
  var score = integer("score")
  var author = text("nick")
  var author_id = integer("author_id")
  var attemptId = text("attempt_id").nullable()
  var attemptUserId = integer("user_id")
  var status = text("status").nullable()
  var testingStartTs = date("testing_start_ts").nullable()
  var count = integer("count")
  var errorMsg = text("error_msg").nullable()
  var resultSet = text("result_set").nullable()
}

object AvailableContests : Table("Contest.AvailableContestDto") {
  val user_id = integer("user_id")
  val contest_code = text("contest_code")
}

class TaskAttempt(val entity: TaskAttemptEntity) {
  val id: Int get() = entity.taskEntity.id
  val name: String get() = entity.taskEntity.name
  val status: String? get() = entity.status
  val testingStartMs: DateTime? get() = entity.testingStartMs
  val count: Int get() = entity.count

  companion object Factory {
    fun fromRow(attemptRow: ResultRow): TaskAttempt {
      return TaskAttempt(TaskAttemptEntity(
          taskEntity = TaskEntity(
              id = attemptRow[AttemptView.taskId],
              name = attemptRow[AttemptView.name],
              score = attemptRow[AttemptView.score],
              description = attemptRow[AttemptView.description],
              signature = null,
              difficulty = attemptRow[AttemptView.difficulty],
              authorName = attemptRow[AttemptView.author]
          ),
          userId = attemptRow[AttemptView.attemptUserId],
          status = attemptRow[AttemptView.status],
          count = attemptRow[AttemptView.count],
          attemptId = attemptRow[AttemptView.attemptId],
          testingStartMs = attemptRow[AttemptView.testingStartTs],
          errorMsg = attemptRow[AttemptView.errorMsg],
          resultSet = attemptRow[AttemptView.resultSet]
      ))
    }
  }

  var assessorResponse: AssessmentPubSubResp? = null
    set(value) {
      val attemptId = this.entity.attemptId
      if (value != null && attemptId != null) {
        UserStorage.exec {
          val user = findUserById(this@TaskAttempt.entity.userId) ?: return@exec
          user.recordAssessment(
              attemptId = attemptId,
              score = value.score,
              errorMsg = value.errors,
              resultSet = ObjectMapper().writeValueAsString(value.resultLines)
          )
        }
      }
    }

}

class Task(val entity: TaskEntity)

class Contest(val code: String, val name: String)

class User(val entity: UserEntity, val txn: Transaction, val storage: UserStorage) {
  private val jsonMapper = ObjectMapper()
  val password: String
    get() = entity.passwd
  val name: String
    get() = entity.name
  val id: Int get() = entity.id

  fun attempts(): List<TaskAttempt> {
    return transaction {
      AttemptView.select { AttemptView.attemptUserId.eq(entity.id) }.map(TaskAttempt.Factory::fromRow)
    }
  }

  val availableTasks: List<AuthorChallengeEntity>
    get() {
      return transaction {
        TaskByAuthorView.select { TaskByAuthorView.authorId.neq(entity.id) }.orderBy(TaskByAuthorView.gainTotal, isAsc = false).map { taskRow ->
          AuthorChallengeEntity(
              authorId = taskRow[TaskByAuthorView.authorId],
              authorName = taskRow[TaskByAuthorView.authorName],
              easyCount = taskRow[TaskByAuthorView.count1],
              mediumCount = taskRow[TaskByAuthorView.count2],
              difficultCount = taskRow[TaskByAuthorView.count3],
              gainTotal = taskRow[TaskByAuthorView.gainTotal]
          )
        }
      }
    }

  fun availableContests(): List<Contest> {
    val contests = transaction {
      AvailableContests.select {
        AvailableContests.user_id eq entity.id
      }.map { it[AvailableContests.contest_code] }.toList()
    }
    return transaction {
      Contests.select {
        Contests.code inList contests
      }.map { Contest(it[Contests.code], it[Contests.name]) }.toList()
    }
  }

  fun acceptRandomChalenges() {
    return storage.procedure("SELECT Contest.AcceptRandomAuthor(?)") {
      setInt(1, this@User.id)
      execute()
    }
  }

  fun createChallengeOffer(difficulty: Int, authorId: Int?): ChallengeOffer {
    val attemptedQuery = QueryAlias(AttemptView.select { AttemptView.attemptUserId.eq(this@User.id) }, "A")
    val selectedQuery = if (authorId == null) {
      QueryAlias(TaskTable.select {TaskTable.difficulty.eq(difficulty)}, "T")
    } else {
      QueryAlias(TaskTable.select {TaskTable.difficulty.eq(difficulty).and(TaskTable.authorId.eq(authorId))}, "T")
    }
    val resultSet = selectedQuery.join(attemptedQuery, JoinType.LEFT,
          onColumn = selectedQuery[TaskTable.id],
          otherColumn = attemptedQuery[AttemptView.taskId]
    ).select { attemptedQuery[AttemptView.taskId].isNull() }.map { it -> it }
    return if (resultSet.isEmpty()) {
      ChallengeOffer(taskId = -1, description = "Кажется, вы уже решаете эту задачу")
    } else {
      val randomRow = resultSet[Random().nextInt(resultSet.size)]
      ChallengeOffer(taskId = randomRow[selectedQuery[TaskTable.id]], description = randomRow[selectedQuery[TaskTable.description]])
    }
  }

  fun acceptChallenge(taskId: Int): Boolean {
    return storage.procedure("SELECT Contest.MakeAttempt(?, ?)") {
      setInt(1, this@User.id)
      setInt(2, taskId)
      execute()
    }
  }

  fun recordAttempt(taskId: Int, attemptId: String): Boolean {
    return storage.procedure("SELECT Contest.StartAttemptTesting(?, ?, ?)") {
      setInt(1, this@User.id)
      setInt(2, taskId)
      setString(3, attemptId)
      execute()
    }
  }

  fun recordAssessment(attemptId: String, score: Int, errorMsg: String?, resultSet: String?) {
    return storage.procedure("SELECT Contest.RecordAttemptResult(?, ?, ?, ?)") {
      setString(1, attemptId)
      setBoolean(2, score > 0)
      if (errorMsg != null) {
        setString(3, errorMsg)
      } else {
        setNull(3, Types.LONGVARCHAR)
      }
      if (resultSet != null) {
        println("Записываем result set=${resultSet}")
        setString(4, resultSet)
      } else {
        setNull(4, Types.LONGVARCHAR)
      }
      execute()
    }
  }
}

class UserStorage(val txn: Transaction) {
  fun createUser(newName: String, newPassword: String): User? {
    return procedure("SELECT id, nick, name, passwd FROM Contest.GetOrCreateContestUser(?,?,?)") {
      setString(1, newName)
      setString(2, newPassword)
      setBoolean(3, true)
      executeQuery().use {
        if (it.next()) {
          User(UserEntity(
              id = it.getInt("id"),
              name = it.getString("name"),
              nick = it.getString("nick"),
              passwd = it.getString("passwd")), txn, this@UserStorage)
        } else {
          null
        }
      }
    }
  }

  fun fromRow(userRow: ResultRow): User {
    return User(UserEntity(
        id = userRow[UserTable.id],
        name = userRow[UserTable.name],
        nick = userRow[UserTable.nick],
        passwd = userRow[UserTable.passwd]
    ), txn, this@UserStorage)
  }

  fun findUserById(id: Int): User? {
    return UserTable.select { UserTable.id.eq(id) }.map(this::fromRow).firstOrNull()
  }

  fun findUser(name: String): User? {
    return UserTable.select { UserTable.name.eq(name) }.map(this::fromRow).firstOrNull()
  }

  fun findTask(id: Int): Task? {
    return TaskTable.select{ TaskTable.id.eq(id) }.map { taskRow ->
      Task(TaskEntity(
          id = taskRow[TaskTable.id],
          name = taskRow[TaskTable.name],
          description = taskRow[TaskTable.description],
          signature = taskRow[TaskTable.signature],
          score = taskRow[TaskTable.score],
          difficulty = taskRow[TaskTable.difficulty],
          authorName = ""
      ))
    }.firstOrNull()
  }

  fun findAttempt(attemptId: String): TaskAttempt? {
    return AttemptView.select { AttemptView.attemptId.eq(attemptId) }.map(TaskAttempt.Factory::fromRow).firstOrNull()
  }

  fun <T> procedure(sqlCall: String, closure: PreparedStatement.() -> T): T {
    val conn = txn.connection
    conn.prepareCall("SET search_path=contest").execute()
    val stmt = conn.prepareStatement(sqlCall)
    return stmt.closure()
  }

  companion object {
    fun <T> exec(code: UserStorage.() -> T): T {
      return transaction {
        try {
          (UserStorage(this)).code()
        } catch (ex: Exception) {
          println(ex)
          ex.printStackTrace()
          throw ex
        }
      }
    }
  }
}

