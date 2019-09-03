package com.bardsoftware.sqool.contest.storage

import com.bardsoftware.sqool.contest.admin.Contests
import com.bardsoftware.sqool.contest.admin.DbQueryManager
import com.bardsoftware.sqool.grader.AssessmentPubSubResp
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import org.joda.time.DateTime
import java.math.BigDecimal
import java.sql.PreparedStatement
import java.sql.Types
import java.util.Random

data class ChallengeOffer(val taskId: Int, val description: String)
data class UserEntity(var id: Int, var nick: String, var name: String, var passwd: String, var isAdmin: Boolean)
data class TaskAttemptEntity(
    var taskEntity: TaskEntity,
    var userId: Int,
    var count: Int,
    var attemptId: String?,
    var status: String?,
    var testingStartMs: DateTime?,
    var errorMsg: String?,
    var resultSet: String?
) {
  companion object Factory {
    fun fromRow(attemptRow: ResultRow) = TaskAttemptEntity(
        taskEntity = TaskEntity(
            id = attemptRow[AttemptView.taskId],
            name = attemptRow[AttemptView.name],
            score = attemptRow[AttemptView.score],
            description = attemptRow[AttemptView.description],
            signatureJson = attemptRow[AttemptView.signature],
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
    )
  }
}


data class TaskEntity(
    var id: Int,
    var name: String,
    var signatureJson: String?,
    var description: String?,
    var score: Int,
    var difficulty: Int,
    var authorName: String
)

data class AuthorChallengeEntity(
    var authorId: Int,
    var authorName: String,
    var easyCount: Int,
    var mediumCount: Int,
    var difficultCount: Int,
    var gainTotal: BigDecimal
)

object UserTable : Table("Contest.ContestUser") {
  var id = integer("id")
  var nick = text("nick")
  var name = text("name")
  var passwd = text("passwd")
  var isAdmin = bool("is_admin")
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

/**
 * Interface to MyAttempts view which shows the state of user task attempts.
 */
object AttemptView : Table("Contest.MyAttempts") {
  var taskId = integer("task_id")
  var name = text("name")
  var description = text("description")
  var signature = text("signature")
  var difficulty = integer("difficulty")
  var score = integer("score")
  var variantId = integer("variant_id")
  var author = text("author_nick")
  var author_id = integer("author_id")
  var attemptId = text("attempt_id").nullable()
  var attemptUserId = integer("user_id")
  var status = text("status").nullable()
  var testingStartTs = date("testing_start_ts").nullable()
  var count = integer("count")
  var errorMsg = text("error_msg").nullable()
  var resultSet = text("result_set").nullable()
}

/**
 * Interface to AvailableContests view which maps users to contests which they can join
 */
object AvailableContests : Table("Contest.AvailableContests") {
  val user_id = integer("user_id")
  val contest_code = text("contest_code")
  val contest_name = text("contest_name")
  val variant_choice = customEnumeration(
      "variant_choice", "VariantChoice",
      { value -> Contests.VariantChoice.valueOf(value.toString()) },
      { Contests.PGEnum("VariantChoice", it) }
  )
  val variants_json_array = text("variants_json_array")
  val assigned_variant_id = integer("assigned_variant_id").nullable()
}

/**
 * Represents student's attempt to solve a task. Contains fields and methods
 * indicating attempt's testing status and response from the assessment system.
 */
class TaskAttempt(val entity: TaskAttemptEntity) {
  val id: Int get() = entity.taskEntity.id
  val name: String get() = entity.taskEntity.name
  val status: String? get() = entity.status
  val testingStartMs: DateTime? get() = entity.testingStartMs
  val count: Int get() = entity.count

  companion object Factory {
    fun fromRow(attemptRow: ResultRow) = TaskAttempt(TaskAttemptEntity.fromRow(attemptRow))
  }

  /**
   * Write-only field for recording assessment response.
   */
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

/**
 * Represents contest which some user joined or may join. Most of the fields are not
 * tied to user, however chosenVariant indicates the variant which is assigned to the
 * context user.
 */
data class Contest(
    val code: String,
    val name: String,
    val variantPolicy: String,
    val variants: List<Variant>,
    val chosenVariant: Variant?
)

data class Variant(val id: Int = -1, val name: String = "")

/**
 * Represents a user and provides accessors to objects related to participation of this user
 * in contests, such as the list of available contests, available tasks, etc.
 */
class User(val entity: UserEntity, val storage: UserStorage) {
  private val queryManager = DbQueryManager()
  val password: String
    get() = entity.passwd
  val name: String
    get() = entity.name
  val id: Int get() = entity.id
  val isAdmin: Boolean
    get() = entity.isAdmin

  /**
   * Returns the list of all attempts made by this user.
   */
  fun attempts(): List<TaskAttempt> {
    return transaction {
      AttemptView.select { AttemptView.attemptUserId.eq(entity.id) }.map(TaskAttempt.Factory::fromRow)
    }
  }

  val availableTasks: List<AuthorChallengeEntity>
    get() {
      return transaction {
        TaskByAuthorView.select { TaskByAuthorView.authorId.neq(entity.id) }
            .orderBy(TaskByAuthorView.gainTotal, SortOrder.DESC)
            .map { taskRow ->
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

  /**
   * Returns the list of all contests available to this user
   */
  fun availableContests(): List<Contest> = transaction {
    AvailableContests.select { AvailableContests.user_id eq entity.id }
          .map { c ->
            val contestCode = c[AvailableContests.contest_code]
            val contestName = c[AvailableContests.contest_name]
            val chosenVariantId = c[AvailableContests.assigned_variant_id]

            val variants = ObjectMapper().readValue(c[AvailableContests.variants_json_array], object : TypeReference<List<Variant>>() {})
            val chosenVariant = chosenVariantId?.let { id ->
              variants.find { it.id == id }
            }
            Contest(contestCode, contestName, c[AvailableContests.variant_choice].name, variants, chosenVariant)
          }.toList()
    }

  /**
   * Assigns the given variant from the given contest to this user and adds attempts for all
   * tasks in that variant in "virgin" state.
   */
  fun assignVariant(contestCode: String, variantId: Int) {
    storage.procedure("SELECT Contest.AssignVariant(?, ?, ?)") {
      setInt(1, this@User.id)
      setString(2, contestCode)
      setInt(3, variantId)
      execute()
    }
  }

  /**
   * Returns all attempts made by this user for tasks from the given variant.
   */
  fun getVariantAttempts(variantId: Int): List<TaskAttemptEntity> = transaction {
    AttemptView.select { (AttemptView.attemptUserId eq this@User.id) and (AttemptView.variantId eq variantId) }
        .map(TaskAttemptEntity.Factory::fromRow)
  }

  /**
   * Returns all attempts made by this user for all tasks from the given contest.
   */
  fun getAllVariantsAttempts(contestCode: String): List<TaskAttemptEntity> = transaction {
    val taskIdList = queryManager.listContestTasksId(contestCode)
    AttemptView.select { (AttemptView.attemptUserId eq this@User.id) and (AttemptView.taskId inList taskIdList) }
        .map(TaskAttemptEntity.Factory::fromRow)
  }

  /**
   * Assigns random variant from the given contest to this user and creates new virgin attempts
   * for all tasks from that variant.
   */
  fun assignRandomVariant(contestCode: String): Int {
    val variantId = queryManager.listContestVariantsId(contestCode).random()
    assignVariant(contestCode, variantId)
    return variantId
  }

  /**
   * Selects a random task with the given difficulty authored by the given aurhor.
   * This is supposed to be used in peer challenge contests where contestants
   * act as both task authors and solvers.
   */
  fun createChallengeOffer(difficulty: Int, authorId: Int?): ChallengeOffer {
    val attemptedQuery = QueryAlias(AttemptView.select { AttemptView.attemptUserId.eq(this@User.id) }, "A")
    val selectedQuery = if (authorId == null) {
      QueryAlias(TaskTable.select { TaskTable.difficulty.eq(difficulty) }, "T")
    } else {
      QueryAlias(TaskTable.select { TaskTable.difficulty.eq(difficulty).and(TaskTable.authorId.eq(authorId)) }, "T")
    }
    val resultSet = selectedQuery.join(attemptedQuery, JoinType.LEFT,
        onColumn = selectedQuery[TaskTable.id],
        otherColumn = attemptedQuery[AttemptView.taskId]
    ).select { attemptedQuery[AttemptView.taskId].isNull() }.map { it }
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

  fun getAssignedVariant(contestCode: String): Int? = transaction {
    val variant = AvailableContests.select { (AvailableContests.user_id eq this@User.id) and (AvailableContests.contest_code eq contestCode) }
        .map { it[AvailableContests.assigned_variant_id] }
        .toList()
    when (variant.size) {
      0 -> throw NoSuchAvailableContestException()
      1 -> variant.first()
      else -> throw Exception("Get more than one available contest by (user_id, contest_code)")
    }
  }

  /**
   * Records that student submitted a new solution which was sent to the assesment service and
   * is being tested now. Removes previously saved assessment details, if any, and sets attempt
   * status to "testing"
   */
  fun recordAttempt(taskId: Int, variantId: Int, attemptId: String): Boolean {
    return storage.procedure("SELECT Contest.StartAttemptTesting(?, ?, ?, ?)") {
      setInt(1, this@User.id)
      setInt(2, taskId)
      setInt(3, variantId)
      setString(4, attemptId)
      execute()
    }
  }

  /**
   * Records the result of testing received from the assessment service.
   */
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
        println("Записываем result set=$resultSet")
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
    return procedure("SELECT id, nick, name, passwd, is_admin FROM Contest.GetOrCreateContestUser(?,?,?)") {
      setString(1, newName)
      setString(2, newPassword)
      setBoolean(3, true)
      executeQuery().use {
        if (it.next()) {
          User(UserEntity(
              id = it.getInt("id"),
              name = it.getString("name"),
              nick = it.getString("nick"),
              passwd = it.getString("passwd"),
              isAdmin = it.getBoolean("is_admin")), this@UserStorage)
        } else {
          null
        }
      }
    }
  }

  private fun fromRow(userRow: ResultRow): User {
    return User(UserEntity(
        id = userRow[UserTable.id],
        name = userRow[UserTable.name],
        nick = userRow[UserTable.nick],
        passwd = userRow[UserTable.passwd],
        isAdmin = userRow[UserTable.isAdmin]
    ), this@UserStorage)
  }

  fun findUserById(id: Int): User? {
    return UserTable.select { UserTable.id.eq(id) }.map(this::fromRow).firstOrNull()
  }

  fun findUser(name: String): User? {
    return UserTable.select { UserTable.name.eq(name) }.map(this::fromRow).firstOrNull()
  }

  fun findTask(id: Int): Task? {
    return TaskTable.select { TaskTable.id.eq(id) }.map { taskRow ->
      Task(TaskEntity(
          id = taskRow[TaskTable.id],
          name = taskRow[TaskTable.name],
          description = taskRow[TaskTable.description],
          signatureJson = taskRow[TaskTable.signature],
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

class NoSuchAvailableContestException : Exception()
