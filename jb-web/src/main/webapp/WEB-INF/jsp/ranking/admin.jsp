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

<spring:url value="/ranking/${serverId}" var="actionUrl" />

<form:form class="form-horizontal" method="post" modelAttribute="config" action="${actionUrl}">
    <div class="row">
        <div class="col-md-6">
            <div class="box box-warning">
                <div class="box-header with-border">
                    <h3 class="box-title">Рейтинг пользователей</h3>
                    <div class="box-tools pull-right">
                        <form:checkbox path="enabled" data-toggle="toggle" data-onstyle="warning" data-size="small" data-on="Вкл" data-off="Выкл" />
                    </div>
                </div>
                <div class="box-body">
                    <spring:bind path="announcement">
                        <div class="callout callout-info">
                            <p>Используйте ключевое слово <code>{user}</code> для вставки обращения к пользователю, а <code>{level}</code> для номера уровня.</p>
                            <p>Общая таблица рейтингов доступна по текущей ссылке для неавторизованных пользователей, а также <a href="${actionUrl}?forceUser=true" target="_blank">здесь.</a></p>
                            <p><span class="text-bold">Команды: </span> <span class="label label-warning">${prefix}<spring:message code="discord.command.rank.key" /></span> <span class="label label-warning">${prefix}<spring:message code="discord.command.leaders.key" /></span></p>
                        </div>
                        <div class="form-group ${status.error ? 'has-error' : ''}">
                            <div class="col-md-12">
                                <form:textarea path="announcement" cssClass="form-control" rows="5"
                                               placeholder="Ура! {user} только что достиг `{level}` уровня! Мои поздравления ^•^" />
                                <form:errors path="announcement" class="help-block" />
                            </div>
                        </div>
                    </spring:bind>

                    <div class="form-group">
                        <label for="announcementEnabled" class="col-sm-4 control-label">Объявление уровня</label>
                        <div class="col-sm-2">
                            <form:checkbox id="announcementEnabled" path="announcementEnabled" data-toggle="toggle"
                                           data-onstyle="warning" data-size="small" data-on="Вкл" data-off="Выкл" />
                        </div>
                        <div class="col-sm-6">
                            <div class="row">
                                <label for="whisper" class="col-sm-6 control-label">В личку</label>
                                <div class="col-sm-6">
                                    <form:checkbox id="whisper" path="whisper" data-toggle="toggle"
                                                   data-onstyle="warning" data-size="small" data-on="Вкл" data-off="Выкл" />
                                </div>
                            </div>
                        </div>
                    </div>

                    <div class="form-group">
                        <label for="resetOnLeave" class="col-sm-5 control-label">Сбрасывать вышедших пользователей</label>
                        <div class="col-sm-7">
                            <form:checkbox id="resetOnLeave" path="resetOnLeave" data-toggle="toggle"
                                           data-onstyle="warning" data-size="small" data-on="Вкл" data-off="Выкл" />
                        </div>
                    </div>

                    <div class="form-group ${status.error ? 'has-error' : ''}">
                        <label for="bannedRoles" class="col-sm-5 control-label">Игнорируемые роли
                            <i class="fa fa-fw fa-question-circle" data-toggle="tooltip" title="Не подсчитывают опыт" data-container="body"></i>
                        </label>
                        <div class="col-sm-7">
                            <form:select id="bannedRoles" path="bannedRoles" disabled="${not serverAdded}"
                                         cssClass="form-control select2" cssStyle="width: 100%;"
                                         items="${roles}" itemValue="idLong" itemLabel="name" multiple="multiple" />
                            <form:errors path="bannedRoles" class="help-block" />
                        </div>
                    </div>
                </div>
            </div>
        </div>
        <div class="col-md-6">
            <div class="box box-warning">
                <div class="box-header with-border">
                    <h3 class="box-title">Награды за уровни</h3>
                </div>
                <div class="box-body">
                    <c:if test="${serverAdded && rolesManageable}">
                        <div class="callout callout-info">
                            <p>После того, как наградная роль была выдана пользователю, снять ее можно только вручную даже если награда за нее уже снята.</p>
                        </div>
                        <table id="rewards-table" class="table table-striped table-hover" width="100%">
                            <thead>
                            <tr>
                                <th>Роль</th>
                                <th>Уровень (0-999)</th>
                            </tr>
                            </thead>
                            <tbody>
                            <c:forEach items="${roles}" var="role" varStatus="i" begin="0">
                                <tr>
                                    <td>
                                        <input name="rewards[${i.index}].roleId" type="hidden" value="${role.idLong}">
                                        <h5 style="font-weight: bold; color:${jb:getHTMLAwtColor(role.color)}">${role.name}</h5>
                                    </td>
                                    <td>
                                        <input name="rewards[${i.index}].level"
                                               type="number" max="999" min="0"
                                               value="${jb:getLevelForRole(config, role.idLong)}"
                                               class="form-control level-input"
                                               placeholder="Без награды"/>
                                    </td>
                                </tr>
                            </c:forEach>
                            </tbody>
                        </table>
                    </c:if>
                    <c:if test="${serverAdded && not rolesManageable}">
                        <div class="callout callout-warning">
                            <p>Пожалуйста, предоставьте боту право доступа на управление ролями для возможности выдачи наград</p>
                        </div>
                    </c:if>
                    <c:if test="${not serverAdded}">
                        <div class="callout callout-warning">
                            <p>Для редактирования списка наград необходимо добавить бота на данный сервер</p>
                        </div>
                    </c:if>
                </div>
            </div>
        </div>
    </div>
    <div class="row">
        <div class="col-md-6">
            <button type="submit" class="btn bg-orange" style="margin-bottom: 15px;">Сохранить изменения</button>
        </div>
    </div>
    <div class="row">
        <div class="col-md-12">
            <div class="box box-warning">
                <div class="box-header">
                    <h3 class="box-title">Список пользователей</h3>
                    <div class="box-tools pull-right">
                        <button id="ranking-import-button" type="button" class="btn btn-info">Импорт из Mee6</button>
                        <button id="ranking-reset-button" type="button" class="btn btn-danger">Сбросить все</button>
                        <button id="ranking-sync-button" type="button" class="btn btn-warning" <c:if test="${not serverAdded}">disabled</c:if>>
                            <span id="ranking-sync-button-text">Обновить</span>
                            <span id="ranking-sync-button-spinner" style="display: none;"><i class="fa fa-circle-o-notch fa-spin" style="font-size:18px;"></i></span>
                        </button>
                    </div>
                </div>
                <div id="ranking-container" class="box-body"></div>
            </div>
        </div>
    </div>
</form:form>

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

<div id="import-content" class="hidden">
    <p>Вы уверены что хотите импортировать прогресс участников сервера из Mee6? Это перезапишет весь текущий прогресс в JuniperBot.</p>
    <p>Прежде чем импортировать, убедитесь что <a href="https://mee6.xyz/levels/${serverId}" target="_blank">рейтинг Mee6</a> доступен.
        Иначе временно <a href="https://mee6.xyz/dashboard/${serverId}/levels" target="_blank">активируйте</a> плагин Levels для возможности импорта.</p>
    <div class="callout callout-warning no-margin">
        <p>Участники, у которых не указана аватарка в Discord, <span class="text-bold">не могут быть импортированы</span>.
            В случае необходимости, можно указать им уровень вручную.</p>
    </div>
</div>