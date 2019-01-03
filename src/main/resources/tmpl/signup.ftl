<html>
<head>
    <meta charset="utf-8" />
    <#include "bootstrap.ftl">
</head>
<body>
<div class="container-fluid">
    <div class="row">
        <h1>Новый пользователь</h1>
        <form class="form-horizontal" method="post" action="/login.do">
            <input type="hidden" name="createIfMissing" value="true"/>
            <input type="hidden" id="name" name="name" value="${name}">
            <input type="hidden" id="password" name="password" value="${password}">
            <div class="text-center">
                <div class="alert alert-warning">
                    Создать пользователя с именем ${name}?
                </div>
                <button class="btn btn-info" type="submit">Да, создать!</button>&nbsp;<a href="/" class="btn btn-default">Нет, я передумал!</a>
            </div>
        </form>
    </div>
</div>
</body></html>
