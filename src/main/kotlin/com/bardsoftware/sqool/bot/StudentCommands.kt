package com.bardsoftware.sqool.bot

import java.math.BigDecimal

class StudentCommands(tg: ChainBuilder)
fun studentLandingMenu(tg: ChainBuilder, student: Student) {
    getCurrentTeammates(tg.userName).onSuccess { curTeammates ->
        val sumScore = getSumScore(tg.userName)
        tg.reply("""
        |*Hello ${student.name.escapeMarkdown()}\!*
        |
        |*Total score*: ${sumScore.first.escapeMarkdown()}/${sumScore.second.multiply(BigDecimal.valueOf(10)).escapeMarkdown()}
        |*Breakdown by sprints:* ${getScoreList(tg.userName).escapeMarkdown()}
        |*Current Team №${curTeammates.teamNum}*: ${curTeammates.members.map { it.displayName.escapeMarkdown() }.joinToString()}
        |[Current repository](https://github.com/dbms-class-2023/project${curTeammates.teamNum})
        |
    """.trimMargin(), isMarkdown = true)
    }.onFailure {
        tg.reply("You are not yet a member of any team", isMarkdown = false)
    }

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
