<!DOCTYPE html>
<html>
<head>
    <#include "bootstrap.ftl">
    <#include "commonCss.ftl">
    <title>Контест Марсофлота</title>
    <style type="text/css">
        .accepted-challenge-tpl {
            display: none;
        }
    </style>
    <meta charset="utf-8"/>
    <script type="application/javascript">
        window.awaitTesting = ${awaitTesting?c};
    </script>
</head>
<body>
<div class="page-header">
    <h1>Привет, ${name!"чувак"}! <small><a href="/logout" class="btn btn-small pull-right">Выйти</a></small></h1>
</div>
<div class="container-fluid">
    <div class="row">
        <div class="col-md-6 col-lg-6 col-sm-12">
            <table class="table" id="accepted-challenges">
                <caption>Решённые задачи и задачи в процессе решения</caption>
                <thead>
                <tr><th>Задача</th><th>Сложность</th><th>Стоимость</th><th></th></tr>
                </thead>
                <tbody>
                <tr class="accepted-challenge-tpl">
                    <td><i class="fa fa-info-circle">&nbsp;<a class="name" data-toggle="popover" data-trigger="focus" role="button" tabindex="0"></a></i></td>
                    <td><div class="difficulty"></div></td>
                    <td><div class="score"></div></td>
                    <td>
                        <div class="status status-failure" data-attempt-id="">
                            <a class="submit" href="">Послать ещё решение</a>
                            <div class="explain bg-danger well">
                                <b class="tldr"></b>
                                <p class="error-msg"></p>
                                <a href="javascript:void(0)" class="btn btn-link btn-sm show-result-set">Показать выборку запроса</a>
                            </div>
                        </div>
                        <div class="status status-virgin">
                            <a class="submit" href="">Послать первое решение</a>
                        </div>
                        <div class="status status-testing">
                            <i class="fa fa-cog fa-spin fa-fw"></i>Проверяем...
                        </div>
                        <div class="status status-success">
                            <i class="fa fa-thumbs-up"></i>Решена!
                        </div>
                    </td>
                </tr>
                </tbody>
            </table>
            <div class="no-challenges well text-center">Решённых задач нет</div>
        </div>
        <div class="col-md-6 col-lg-6 col-sm-12">
            <table class="table ddl">
                <caption>&nbsp;</caption>
                <thead><tr><th>Схема БД</th></tr>
                </thead>
                <tbody>
                <tr class="v1 v2 v3"><td><b><a href="/sql/Pies.sql">Пироги</a></b></td></tr>
                <tr class="v4 v5 v6"><td><b><a href="/sql/Coffee.sql">Кофе</a></b></td></tr>
                </tbody>
            </table>
                <table class="table">
                    <caption>Все доступные задачи</caption>
                    <thead>
                    <tr>
                        <th>Автор</th>
                        <th>
                            Легкая
                        </th>
                        <th>
                            Средняя
                        </th>
                        <th>
                            Сложная
                        </th>
                    </tr>
                    </thead>
                    <tbody>
                    <#list tasks as task>
                    <tr>
                        <td>
                            ${task.authorName}
                        </td>
                        <td>
                            <a class="author-this" data-difficulty="1" data-author="${task.authorId}" href="javascript:void(0)">${task.easyCount} задачи</a>
                        </td>
                        <td>
                            <a class="author-this" data-difficulty="2" data-author="${task.authorId}" href="javascript:void(0)">${task.mediumCount} задачи</a>
                        </td>
                        <td>
                            <a class="author-this" data-difficulty="3" data-author="${task.authorId}" href="javascript:void(0)">${task.difficultCount} задачи</a>
                        </td>
                    </tr>
                    <#else>
                    <tr><td colspan="4"><div class="well text-center">Нерешённых задач нет</div></td> </tr>
                    </#list>
                    </tbody>
                </table>
            </div>
    </div>
    <div class="row">
        <div class="alert alert-danger hide" role="alert">
            <p></p>
        </div>
    </div>
</div>

<div class="modal fade" id="accept-challenge-modal" tabindex="-1" role="dialog">
    <div class="modal-dialog" role="document">
        <div class="modal-content">
            <div class="modal-header">
                <button type="button" class="close" data-dismiss="modal" aria-label="Close"><span aria-hidden="true">&times;</span></button>
                <h4 class="modal-title">Действительно хотите решить эту задачу?</h4>
            </div>
            <div class="modal-body">
            </div>
            <div class="modal-footer">
                <button type="button" class="btn btn-default" data-dismiss="modal">Ой, нет</button>
                <button type="button" class="btn btn-primary">А то!</button>
            </div>
        </div>
    </div>
</div>
<div class="modal fade" id="failed-challenge-details" role="dialog">
    <div class="modal-dialog" role="document">
        <div class="modal-content">
            <div class="modal-header">
                <button type="button" class="close" data-dismiss="modal" aria-label="Close"><span aria-hidden="true">&times;</span></button>
                <h4 class="modal-title">Первые строки вашего результата</h4>
            </div>
            <div class="modal-body">
                <table class="table result-set-table">
                    <thead></thead>
                    <tbody></tbody>
                </table>
            </div>
        </div>
    </div>
</div>

<script src="/js/me.js"></script>
<script src="/js/Dashboard.bundle.js" async></script>
</body>
</html>
