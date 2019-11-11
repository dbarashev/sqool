<!DOCTYPE html>
<html>
<head>
    <meta charset="utf-8">
    <#include "bootstrap.ftl">
</head>
<body>
<div class="container-fluid">
    <div class="row">
        <div class="col-lg-offset-4 col-lg-4 col-md-offset-4 col-md-4 col-xs-12 col-sm-offset2 col-sm-10 container-fluid">
            <h1 class="row">Login</h1>
            <form class="form-horizontal" method="post" action="/login.do">
                <input type="hidden" name="createIfMissing" value="false"/>
                <input type="hidden" name="redirectUrl" value="${redirectUrl}"/>
                <div class="form-group">
                    <label class="col-sm-3 text-right" for="name">Имя</label>
                    <div class="col-sm-9">
                        <input type="text" id="name" name="name">
                    </div>
                </div>
                <div class="form-group">
                    <label class="col-sm-3 text-right" for="password">Пароль</label>
                    <div class="col-sm-9">
                        <input type="password" id="password" name="password">
                    </div>
                </div>
                <div class="form-group">
                    <div class="col-sm-offset-3 col-sm-9">
                        <button type="submit">Войти</button>
                    </div>
                </div>
            </form>
        </div>

    </div>
</div>
</body></html>
