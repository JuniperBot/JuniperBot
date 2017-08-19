<%@ page contentType="text/html;charset=UTF-8" %>
<%@include file="/WEB-INF/template/include.jsp" %>

<div class="jumbotron jumbotron-fluid">
    <div class="container">
        <div class="row">
            <div class="col-md-7">
                <h1 class="jumbo-header">МУЗЫКА, ФЫР И УРУРУ</h1>
                <p class="lead">Добавь Джупи и сделай свой сервер самым фыр-фырным</p>
            </div>
            <div class="col-md-5">
                <a href="#"
                   onclick="window.open('https://discordapp.com/api/oauth2/authorize?client_id=${clientId}&scope=bot&permissions=${permissions}', 'newwindow', 'width=500,height=700'); return false;"
                   class="btn btn-default btn-lg btn-add">
                    Добавить в <img src="<c:url value="/resources/img/discord-logo.svg"/>" class="discord-logo">
                </a>
            </div>
        </div>
    </div>
</div>