package com.bardsoftware.sqool.bot

import java.math.BigDecimal

class StudentCommands(tg: ChainBuilder) {
    init {
        tg.onCommand("t") {
            studentLandingMenu(tg)
        }
    }
}

fun studentLandingMenu(tg: ChainBuilder) {
    getCurrentTeammates(tg.userName).onSuccess { curTeammates ->
        val sumScore = getSumScore(tg.userName)
        tg.reply("""
        |Привет ${tg.userName}!
        |--------
        |
        |**Текущая сумма баллов**: ${sumScore.first.toString()}/${sumScore.second.multiply(BigDecimal.valueOf(10))}
        |**По итерациям:** ${getScoreList(tg.userName)}
        |**Текущая команда №${curTeammates.teamNum}**: ${curTeammates.members.map { it.displayName }.joinToString()}
        |
    """.trimMargin(), isMarkdown = false)
    }.onFailure {
        tg.reply("У вас пока нет команды", isMarkdown = false)
    }

    getPrevTeammates(tg.userName).onSuccess {teammates ->
        val btns = teammates.members.sortedBy { it.ord }.map {mate ->
            BtnData(mate.displayName,
                """${TeammateScoringState(tg.userName, mate.id, teammates.sprintNum, DIALOG_STARTED).toJsonString()}"""
            )
        }
        tg.reply("Ваша команда на прошлой итерации. Если ткнуть в кнопку, можно поставить оценку", stop = true, buttons = btns, isMarkdown = false, maxCols = 1)
    }.onFailure {
        tg.reply("Кажется, вы не участвовали в прошлой итерации", isMarkdown = false)
    }
}
