<html>
<head>
    <meta charset="utf-8" />
    <#include "bootstrap.ftl">
</head>
<body>
<div class="container-fluid mt-5">
    <div class="row  justify-content-md-center">
        <form class="form-horizontal" method="post" action="/login.do">
            <input type="hidden" name="createIfMissing" value="true"/>
            <input type="hidden" id="email" name="email" value="${email}">
            <input type="hidden" id="name" name="name" value="${name}">
            <input type="hidden" id="password" name="password" value="${password}">
            <input type="hidden" name="redirectUrl" value="${redirectUrl}"/>
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
