<%--
This file is part of JuniperBotJ.

JuniperBotJ is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

JuniperBotJ is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with JuniperBotJ. If not, see <http://www.gnu.org/licenses/>.
--%>
<%@ page contentType="text/html;charset=UTF-8" %>
<%@include file="/WEB-INF/template/include.jsp" %>

<div class="box box-widget widget-server <c:if test="${not empty serverIcon}">bg-image</c:if>">
    <div class="widget-server-header bg-yellow">
        <div class="widget-server-background" style="background-image: url('/api/blur?source=<c:url value="${serverIcon}"/>');">
            <div class="widget-bottom-panel"></div>
        </div>
        <div class="row widget-server-header-row">
            <div class="col-md-7">
                <div class="pull-left widget-header-name">
                    <h5 class="widget-server-title">Рейтинг участников</h5>
                    <h3 class="widget-server-name">${serverName}</h3>
                </div>
            </div>
            <div class="col-md-5">
                <div class="callout pull-right">
                    <h4 class="pull-left">Как это работает?</h4>
                    <c:if test="${not empty rewards}">
                        <div class="dropdown pull-right">
                            <a href="#" class="dropdown-toggle" data-toggle="dropdown" data-container="body"
                               aria-expanded="true">
                                <h4>
                                    <span>Список наград</span>
                                    <span class="caret"></span>
                                </h4>
                            </a>
                            <div class="dropdown-menu rewards-menu">
                                <table class="table table-striped table-hover">
                                    <thead>
                                    <tr>
                                        <th>Роль</th>
                                        <th>Уровень</th>
                                    </tr>
                                    </thead>
                                    <tbody>
                                    <c:forEach items="${rewards}" var="reward">
                                        <tr>
                                            <td><span style="font-weight: bold; color:${jb:getHTMLAwtColor(reward.role.color)}">${reward.role.name}</span></td>
                                            <td>${reward.level}</td>
                                        </tr>
                                    </c:forEach>
                                    </tbody>
                                </table>
                            </div>
                        </div>
                    </c:if>
                    <div class="clearfix"></div>
                    <p>Каждый раз, когда ты пишешь что-нибудь в чат, ты получаешь от 15 до 25 единиц опыта <span class="text-bold">не более раза в минуту</span>. На самом сервере введи команду <span class="label label-warning">${prefix}<spring:message code="discord.command.rank.key" /></span> для ознакомления со своим прогрессом.</p>
                </div>
            </div>
        </div>
    </div>

    <div class="widget-server-image">
        <img class="img-circle" src="${serverIcon}" alt="Аватар сервера">
    </div>
    <div class="box-footer">
        <div id="ranking-container" class="widget-server-container"></div>
    </div>
</div>
<script type="text/javascript" src="<c:url value="/resources/js/ranking.js?v=${buildTimestamp}"/>"></script>
<script type="text/javascript">
    $(document).ready(function () {
        var ranking = new Ranking();
        ranking.init();
    });
</script>

<div id="update-level-modal" class="modal bootstrap-dialog type-warning fade" tabindex="-1" role="dialog" aria-labelledby="update-level-modal-label">
    <div class="modal-dialog" role="document">
        <div class="modal-content">
            <div class="modal-header">
                <div class="bootstrap-dialog-header">
                    <div class="bootstrap-dialog-close-button">
                        <button class="close" aria-label="close">×</button></div>
                    <div id="update-level-modal-label" class="bootstrap-dialog-title" id="e3384762-1886-42af-b99a-1b37d8134022_title">Сброс прогресса пользователя "Карамелька"</div>
                </div>
            </div>
            <div class="modal-body">
                <p>Укажите новый уровень пользователя от 0 до 999 в поле ниже и нажмите кнопку "Продолжить":</p>
            </div>
            <div class="modal-footer">
                <div class="col-md-9">
                    <div class="row">
                        <div class="form-group col-md-6">
                            <input id="update-level-value" type="number" max="999" min="1" class="form-control" placeholder="Уровень пользователя" />
                        </div>
                    </div>
                </div>
                <div class="col-md-2">
                    <button id="update-level-button" type="button" class="btn btn-warning" style="width: 110px;">
                        <span id="update-level-text">Обновить</span>
                        <span id="update-level-spinner" style="display: none;"><i class="fa fa-circle-o-notch fa-spin" style="font-size:18px;"></i></span>
                    </button>
                </div>
            </div>
        </div>
    </div>
</div>