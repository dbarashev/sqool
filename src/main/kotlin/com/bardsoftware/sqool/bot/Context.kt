import com.bardsoftware.libbotanique.BtnData
import com.bardsoftware.libbotanique.ChainBuilder
import com.bardsoftware.sqool.bot.getAction
import com.bardsoftware.sqool.bot.getUniversity
import com.bardsoftware.sqool.bot.isTeacher
import com.bardsoftware.sqool.bot.sprintNumbers
import com.fasterxml.jackson.databind.node.ObjectNode
import com.github.michaelbull.result.*

data class InitContext(val tg: ChainBuilder, val json: ObjectNode, val action: Int)
data class UniContext(val value: Int, val flow: InitContext)
data class SprintContext(val value: Int, val uni: UniContext)

const val NO_ACTION = -1
fun ChainBuilder.initContext(json: ObjectNode = this.callbackJson, action: Int = this.callbackJson.getAction().getOr(-1)) = Ok(InitContext(this, json, action))

internal fun Ok<InitContext>.whenTeacher(): Result<InitContext, String> =
  if (isTeacher(this.value.tg.userName)) this else Err("You are not a teacher")

internal fun InitContext.whenLanding(code: () -> Unit) {
  json.getAction().mapError { code() }
}

internal fun InitContext.withUniversity(): Result<UniContext, String> =
  this.json.getUniversity().map { uni -> UniContext(uni, this) }
    .mapError {
      this.tg.reply("Выберите вуз", buttons = listOf(
        BtnData("CUB", """ {"u": 0, "p": 1} """)
      ))
      ""
    }

internal fun Result<InitContext, Any>.withUniversity() =
  this.andThen { it.withUniversity() }

internal fun UniContext.withSprint(): Result<SprintContext, Any> =
  flow.json["s"]?.asInt()?.let { sprint ->
    Ok(SprintContext(sprint, this))
  } ?: run {
    val uni = value
    val buttons = sprintNumbers(uni).map {
      BtnData(
        "№${it.component1()}",
        """{"p": ${flow.action}, "s": ${it.component1()}, "u": $uni } """
      )
    } + listOf(BtnData("Весь курс", """{"p": ${flow.action}, "s": -1, "u": $uni } """))
    flow.tg.reply("Выберите итерацию", buttons = buttons, maxCols = 4)
    Err(0)
  }

internal fun Result<UniContext, Any>.withSprint(): Result<SprintContext, Any> =
  this.andThen { uniContext -> uniContext.withSprint()  }

internal fun Result<SprintContext, Any>.execute(code: (tg: ChainBuilder, json: ObjectNode, uni: Int, sprintNum: Int) -> Unit) =
  this.onSuccess {
    code(it.uni.flow.tg, it.uni.flow.json, it.uni.value, it.value)
  }

