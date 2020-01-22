<!DOCTYPE html>
<html>
<head>
    <meta charset="utf-8">
    <meta http-equiv="X-UA-Compatible" content="IE=edge">
    <meta name="viewport" content="width=device-width,initial-scale=1.0">
    <title>SQooL</title>
    <link rel="stylesheet" href="https://unpkg.com/bootstrap-material-design@4.1.1/dist/css/bootstrap-material-design.min.css" integrity="sha384-wXznGJNEXNG1NFsbm0ugrLFMQPWswR3lds2VeinahP8N0zJw9VWSopbjv2x7WCvX" crossorigin="anonymous">
    <link rel="stylesheet" href="https://fonts.googleapis.com/css?family=Roboto:300,400,500,700|Material+Icons">
    <link rel="stylesheet" href="https://maxcdn.bootstrapcdn.com/font-awesome/4.7.0/css/font-awesome.min.css"/>
    <link href="/js/user.js" rel="preload" as="script">
</head>
<body>
<div class="container-fluid mt-5">
    <div class="row">
        <div class="col-lg-offset-4 col-lg-4 col-md-offset-4 col-md-4 col-xs-12 col-sm-offset2 col-sm-10 container-fluid">

            <ul class="nav nav-tabs" role="tablist">
                <li class="nav-item">
                    <a class="nav-link active"
                       data-toggle="tab" role="tab"
                       href="#tab-signin">Вход</a>
                </li>
                <li class="nav-item">
                    <a class="nav-link"
                       data-toggle="tab" role="tab"
                       href="#tab-signup">Регистрация</a>
                </li>
            </ul>
            <div class="tab-content">
                <div class="tab-pane fade show active" id="tab-signin" role="tabpanel">
                    <form method="post" action="/login.do">
                        <input type="hidden" name="action" value="signin"/>
                        <input type="hidden" name="createIfMissing" value="false"/>
                        <input type="hidden" name="redirectUrl" value="${redirectUrl}"/>
                        <div class="form-group">
                            <label for="name" class="bmd-label-floating">Ваше имя</label>
                            <input type="text" class="form-control" id="name" name="name" required>
                        </div>
                        <div class="form-group">
                            <label for="password" class="bmd-label-floating">Пароль</label>
                            <input type="password" class="form-control" id="password" name="password">
                        </div>
                        <button type="submit" class="btn btn-primary btn-raised">Войти</button>
                    </form>
                </div>
                <div class="tab-pane fade" id="tab-signup" role="tabpanel">
                    <form method="post" action="/login.do">
                        <input type="hidden" name="action" value="signup"/>
                        <input type="hidden" name="createIfMissing" value="false"/>
                        <input type="hidden" name="redirectUrl" value="${redirectUrl}"/>
                        <div class="form-group">
                            <label for="name" class="bmd-label-floating">Ваше имя</label>
                            <input type="text" class="form-control" id="name" name="name" required>
                            <small class="form-text text-muted">Имя в настоящей жизни</small>
                        </div>
                        <div class="form-group">
                            <label for="password" class="bmd-label-floating">Пароль</label>
                            <input type="password" class="form-control" id="password" name="password">
                            <small class="form-text text-muted">Да хоть пустой</small>
                        </div>
                        <div class="form-group">
                            <label for="email" class="bmd-label-floating">Адрес электронной почты</label>
                            <input type="email" class="form-control" id="email" name="email" required>
                            <small class="form-text text-muted">Куда будут приходить рецензии от преподавателя</small>
                        </div>
                        <button type="submit" class="btn btn-primary btn-raised">Зарегистрироваться</button>
                    </form>
                </div>
            </div>
        </div>

    </div>
</div>
<script
        src="http://code.jquery.com/jquery-3.3.1.min.js"
        integrity="sha256-FgpCb/KJQlLNfOu91ta32o/NMZxltwRo8QtmkMRdAu8="
        crossorigin="anonymous"></script>
<script
        src="https://unpkg.com/popper.js@1.12.6/dist/umd/popper.js"
        integrity="sha384-fA23ZRQ3G/J53mElWqVJEGJzU0sTs+SvzG8fXVWP+kJQ1lwFAOkcUOysnlKJC33U"
        crossorigin="anonymous"></script>
<script
        src="https://unpkg.com/bootstrap-material-design@4.1.1/dist/js/bootstrap-material-design.js"
        integrity="sha384-CauSuKpEqAFajSpkdjv3z9t8E7RlpJ1UP0lKM/+NdtSarroVKu069AlsRPKkFBz9"
        crossorigin="anonymous"></script>
<script>$(document).ready(function() { $('body').bootstrapMaterialDesign(); });</script>
</body>
</html>

