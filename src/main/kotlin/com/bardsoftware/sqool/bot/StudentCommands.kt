package com.bardsoftware.sqool.bot

import com.bardsoftware.libbotanique.*
import java.math.BigDecimal

class StudentCommands(tg: ChainBuilder)
fun studentLandingMenu(tg: ChainBuilder, student: Student) {
    println("Landing (tgUsername=${tg.userName})")
    val sumScore = getSumScore(tg.userName)
    val scoreMarkup = """
        *Total score*: ${sumScore.first.escapeMarkdown()}/${sumScore.second.multiply(BigDecimal.valueOf(10)).escapeMarkdown()}
        *Breakdown by sprints:* ${getScoreList(tg.userName).escapeMarkdown()}        
    """.trimIndent()
    val teamMarkup = getCurrentTeammates(tg.userName).fold(
        onSuccess = {curTeammates ->
            """
        *Current Team №${curTeammates.teamNum}*: ${curTeammates.members.map { it.displayName.escapeMarkdown() }.joinToString()}
        [Current repository](https://github.com/dbms-class-2023/project${curTeammates.teamNum})                
            """.trimIndent()
        },
        onFailure = {
            "Currently you are not a member of any team"
        }
    )
    tg.reply("""
        *Hello ${student.name.escapeMarkdown()}\!*
        $scoreMarkup
        $teamMarkup
    """.trimIndent(), isMarkdown = true)

    getPrevTeammates(tg.userName).onSuccess {teammates ->
        val btns = teammates.members.sortedBy { it.ord }.filter { it.tgUsername != tg.userName }.map {mate ->
            BtnData(mate.displayName,
                """${TeammateScoringState(tg.userName, mate.id, teammates.sprintNum, DIALOG_STARTED).toJsonString()}"""
            )
        }
        tg.reply("Your team during the last sprint. Click a button to assess your team-mate", stop = true, buttons = btns, isMarkdown = false, maxCols = 1)
    }.onFailure {
        tg.reply("It seems that you didn't participate in the last sprint", isMarkdown = false)
    }
}
