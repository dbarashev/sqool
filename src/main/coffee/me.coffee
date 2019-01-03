reportError= (msg) ->
  $(".alert-danger p").text(msg)
  $(".alert-danger").removeClass("hide").show()
  window.setTimeout((-> $(".alert-danger").fadeOut(500)), 3000)

statusReloads = {}

setStatus = (row, status, attemptId, taskId, attemptCount)->
  row.find(".status").hide()
  row.find(".status-#{status}").show().data("attempt-id", attemptId)
  row.find(".submit").attr("href", "/submit?id=#{taskId}")
  if "failure" == status
    tldr = switch attemptCount
      when 1 then "Кажется, что-то пошло не так."
      when 2 then "Опять нет"
      when 3 then "Да что ж такое!"
      else "Это фиаско, друг!"
    row.find(".tldr").text(tldr)
    explainFailure(row, attemptId)

reloadAcceptedChallengesTable = (challenges) ->
  hasTesting = false
  if challenges.length
    authorName = ""
    $("div.no-challenges").hide()
    $(".accepted-challenge").remove()
    tpl = $(".accepted-challenge-tpl")
    for ch in challenges
      if "testing" == ch.status
        hasTesting = true
      row = tpl.clone()
      row.removeClass("accepted-challenge-tpl").addClass("accepted-challenge")
      row.find(".name").text(ch.taskEntity.name).data("content", ch.taskEntity.description).popover()
      row.find(".difficulty").text(ch.taskEntity.difficulty)
      row.find(".score").text(ch.taskEntity.score)
      setStatus(row, ch.status, ch.attemptId, ch.taskEntity.id, ch.count)
      authorName = ch.taskEntity.authorName # у нас один и тот же автор у всех выбранных задач
      $("#accepted-challenges tbody").append(row)
    $(".ddl tbody tr").hide()
    $(".ddl .#{authorName}").show()
    $(".ddl").show()
  else
    $("div.no-challenges").show()
  refreshTs = window.awaitTesting || hasTesting
  if hasTesting
    window.awaitTesting = false
    window.history.pushState(null, null, "/me")
  if refreshTs
    window.setTimeout((=>
      reloadAcceptedChallenges()
    ), 1000)

reloadStatus = (attemptId) ->
  req = $.ajax(
    url: "/getAttemptStatus?attempt-id=#{attemptId}"
    dataType: "json"
  )
  req.fail(->
    reportError("Не получилось обновить статус задачи")
  )
  return req

reloadAcceptedChallenges = ->
  $.ajax(
    url: "/getAcceptedChallenges"
    dataType: "json"
  ).done((challenges) ->
    reloadAcceptedChallengesTable(challenges)
  ).fail( ->
   reportError("Не получилось загрузить список задач")
  )

explainFailure = (row, attemptId) ->
  reloadStatus(attemptId).done((attempt) ->
    row.find(".error-msg").text(attempt.errorMsg)
    row.find(".show-result-set").on("click", -> showResultSet(attempt))
  )

showResultSet = (attempt) ->
  buildHtmlTable($("#failed-challenge-details .result-set-table"), JSON.parse(attempt.resultSet))
  $("#failed-challenge-details").modal("show")

maybeAcceptChallenge = (doTry) ->
  $("#accept-challenge-modal .modal-body").text(doTry.description)
  $("#accept-challenge-modal").modal("show")
  $("#accept-challenge-modal .btn-primary").off("click").on("click", ->
    $.ajax(
      url: "/try.do"
      data: {
        id: doTry.taskId
      }
    ).done( ->
      reloadAcceptedChallenges()
    ).fail( ->
      reportError("Не получилось взять задачу")
    ).always(->
      $("#accept-challenge-modal").modal("hide")
    )
  )


buildHtmlTable = (selector, myList) ->
  columns = addAllColumnHeaders(myList, selector)
  $("tbody", selector).empty()
  for row in myList
    jqRow = $('<tr/>')
    for colName in columns
      cellValue = row[colName] ? "NULL";
      jqRow.append($('<td/>').html(cellValue))
    $("tbody", selector).append(jqRow)


addAllColumnHeaders = (myList, selector) ->
  $("thead", selector).empty()
  columnSet = [];
  jqHeader = $('<tr/>')
  for key of myList[0]
    if "query_id" != key
      columnSet.push(key)
      jqHeader.append($('<th/>').html(key ? "NULL"))
  $("thead", selector).append(jqHeader)
  return columnSet

$(".author-any").on("click", (e) ->
  dfclt = $(e.target).data("difficulty")
  $.ajax(
    url: "/try"
    data: {
      difficulty: dfclt
    }
    dataType: "json"
  ).done( (doTry) ->
    maybeAcceptChallenge(doTry)
  ).fail( ->
    reportError("Не получилось начать решать задачу")
  )
)
$(".author-this").on("click", (e) ->
  dfclt = $(e.target).data("difficulty")
  author = $(e.target).data("author")
  $.ajax(
    url: "/try"
    data: {
      difficulty: dfclt
      author: author
    }
    dataType: "json"
  ).done( (doTry) ->
    maybeAcceptChallenge(doTry)
  ).fail( ->
    reportError("Не получилось начать решать задачу")
  )
)
window.setTimeout((=>
  reloadAcceptedChallenges()
), 1000)
