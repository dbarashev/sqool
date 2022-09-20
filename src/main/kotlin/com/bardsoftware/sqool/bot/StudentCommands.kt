package com.bardsoftware.sqool.bot

class StudentCommands(tg: ChainBuilder) {
    init {
        tg.onCommand("t") {
            landingMenu(tg)
        }
    }
}

fun landingMenu(tg: ChainBuilder) {
    val curTeammates = getCurrentTeammates(tg.userName)
    tg.reply("Ваша нынешняя команда №${curTeammates.teamNum}: ${curTeammates.members.map { it.displayName }.joinToString()}", isMarkdown = false)

    val teammates = getPrevTeammates(tg.userName)
    val btns = teammates.members.sortedBy { it.ord }.map {mate ->
        BtnData(mate.displayName,
            """${TeammateScoringState(tg.userName, mate.id, teammates.sprintNum, DIALOG_STARTED).toJsonString()}"""
        )
    }
    tg.reply("Ваша команда на прошлой итерации. Если ткнуть в кнопку, можно поставить оценку", stop = true, buttons = btns, isMarkdown = false, maxCols = 1)
}
