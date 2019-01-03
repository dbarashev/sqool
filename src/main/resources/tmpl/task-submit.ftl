<!DOCTYPE html>
<html>
<head>
    <meta charset="utf-8">
    <#include "bootstrap.ftl">
    <#include "commonCss.ftl">
    <title>Задача ${task.name}</title>
</head>
<body>
<div class="page-header">
    <h1>Задача ${task.name}<br><small>Сложность ${task.difficulty}, стоимость ${task.score}</small></h1>
</div>
<div class="container-fluid">
    <div class="row">
        <pre class="well">
            <code>
            ${task.description}
            </code>
        </pre>
        <form action="/submit.do" method="POST">
            <input type="hidden" name="task-id" value="${task.id}"  id="task-id">
            <input type="hidden" name="task-name" value="${task.name}">
            <input type="hidden" name="user-id" value="${user.id}">
            <input type="hidden" name="contest-id" value="${contestId}">
            <div class="form-group">
                <#--<label>Аргументы и возвращаемое значение:</label>-->
                <p class="form-control-static">${task.signature}</p>
                <label for="solution">Текст решения:</label>
                <code>
                    <textarea rows=10 cols=80 name="solution" class="form-control code" id="solution"></textarea>
                </code>
                <#--<div class="alert alert-info">-->
                    <#--<ul>-->
                        <#--<li>Если аргументы есть, то в запросе нужно их использовать.</li>-->
                        <#--<li>Обратите внимание на порядок и тип столбцов в возвращаемом значении.</li>-->
                    <#--</ul>-->
                <#--</div>-->
            </div>
            <button type="submit" class="btn btn-default" id="btn-submit">Послать решение</button>
        </form>
    </div>
</div>
<script src="/js/submit.js"></script>
</body></html>
