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
<%@include file="strings.jsp" %>

<spring:url value="/ranking/${serverId}" var="actionUrl" />

<spring:message code="global.switch.on" var="switchOn"/>
<spring:message code="global.switch.off" var="switchOff"/>

<form:form class="form-horizontal" method="post" modelAttribute="config" action="${actionUrl}">
    <div class="row">
        <div class="col-lg-7 col-md-12">
            <div class="box box-warning">
                <div class="box-header with-border">
                    <h3 class="box-title"><spring:message code="page.ranking.title"/></h3>
                    <div class="box-tools pull-right">
                        <form:checkbox path="enabled"
                                       data-toggle="toggle"
                                       data-onstyle="warning"
                                       data-size="small"
                                       data-on="${switchOn}"
                                       data-off="${switchOff}" />
                    </div>
                </div>
                <div class="box-body">
                    <spring:bind path="announcement">
                        <div class="callout callout-info">
                            <jb:command code="discord.command.rank.key" var="rankCommand"/>
                            <jb:command code="discord.command.leaders.key" var="leadersCommand"/>
                            <p><spring:message code="page.ranking.admin.callout.1"/></p>
                            <p><spring:message code="page.ranking.admin.callout.2" arguments="${actionUrl}"/></p>
                            <p><spring:message code="page.ranking.admin.callout.3" arguments="${prefix};${rankCommand};${leadersCommand}"
                                               argumentSeparator=";"/></p>
                        </div>
                        <div class="form-group ${status.error ? 'has-error' : ''}">
                            <div class="col-md-12">
                                <spring:message code="discord.command.rank.levelup" var="lvlupPlaceholder"/>
                                <form:textarea path="announcement" cssClass="form-control" rows="5"
                                               placeholder="${lvlupPlaceholder}" />
                                <form:errors path="announcement" class="help-block" />
                            </div>
                        </div>
                    </spring:bind>

                    <div class="form-group">
                        <label for="announcementEnabled" class="col-sm-4 control-label">
                            <spring:message code="page.ranking.admin.announce"/>
                        </label>
                        <div class="col-sm-2">
                            <form:checkbox id="announcementEnabled" path="announcementEnabled"
                                           data-toggle="toggle"
                                           data-onstyle="warning"
                                           data-size="small"
                                           data-on="${switchOn}"
                                           data-off="${switchOff}" />
                        </div>
                        <div class="col-sm-6">
                            <div class="row">
                                <label for="whisper" class="col-sm-6 control-label">
                                    <spring:message code="global.button.sendToDM"/>
                                </label>
                                <div class="col-sm-6">
                                    <form:checkbox id="whisper" path="whisper"
                                                   data-toggle="toggle"
                                                   data-onstyle="warning"
                                                   data-size="small"
                                                   data-on="${switchOn}"
                                                   data-off="${switchOff}" />
                                </div>
                            </div>
                        </div>
                    </div>

                    <div class="form-group">
                        <label for="resetOnLeave" class="col-sm-5 control-label">
                            <spring:message code="page.ranking.admin.resetOnLeave"/>
                        </label>
                        <div class="col-sm-7">
                            <form:checkbox id="resetOnLeave" path="resetOnLeave"
                                           data-toggle="toggle"
                                           data-onstyle="warning"
                                           data-size="small"
                                           data-on="${switchOn}"
                                           data-off="${switchOff}" />
                        </div>
                    </div>

                    <div class="form-group ${status.error ? 'has-error' : ''}">
                        <label for="bannedRoles" class="col-sm-5 control-label">
                            <spring:message code="page.ranking.admin.ignoredRoles"/>
                            <i class="fa fa-fw fa-question-circle" data-toggle="tooltip"
                               title="<spring:message code="page.ranking.admin.ignoredRoles.tooltip"/>"
                               data-container="body"></i>
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
        <div class="col-lg-5 col-md-12">
            <div class="box box-warning">
                <div class="box-header with-border">
                    <h3 class="box-title"><spring:message code="page.ranking.rewards"/></h3>
                </div>
                <div class="box-body">
                    <c:if test="${serverAdded && rolesManageable}">
                        <table id="rewards-table" class="table table-striped table-hover" width="100%">
                            <thead>
                            <tr>
                                <th><spring:message code="page.ranking.rewards.role"/></th>
                                <th><spring:message code="page.ranking.rewards.level"/> (0-999)</th>
                            </tr>
                            </thead>
                            <tbody>
                            <c:forEach items="${rolesInteract}" var="role" varStatus="i" begin="0">
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
                                               placeholder="<spring:message code="page.ranking.rewards.none"/>"/>
                                    </td>
                                </tr>
                            </c:forEach>
                            </tbody>
                        </table>
                        <div class="callout callout-info no-margin">
                            <p><spring:message code="page.ranking.admin.rewards.hint"/></p>
                        </div>
                    </c:if>
                    <c:if test="${serverAdded && not rolesManageable}">
                        <div class="callout callout-warning">
                            <p><spring:message code="page.ranking.admin.rewards.noPermissions"/></p>
                        </div>
                    </c:if>
                    <c:if test="${not serverAdded}">
                        <div class="callout callout-warning">
                            <p><spring:message code="page.ranking.admin.rewards.addToServer"/></p>
                        </div>
                    </c:if>
                </div>
            </div>
        </div>
    </div>
    <div class="row">
        <div class="col-md-6">
            <button type="submit" class="btn bg-orange" style="margin-bottom: 15px;">
                <spring:message code="global.button.save"/>
            </button>
        </div>
    </div>
    <div class="row">
        <div class="col-md-12">
            <div class="box box-warning">
                <div class="box-header">
                    <h3 class="box-title"><spring:message code="page.ranking.list.title"/></h3>
                    <div class="box-tools pull-right">
                        <button id="ranking-reset-button" type="button" class="btn btn-danger">
                            <spring:message code="global.button.resetAll"/>
                        </button>
                        <button id="ranking-sync-button" type="button" class="btn btn-warning">
                            <spring:message code="global.button.update"/>
                        </button>
                    </div>
                </div>
                <div class="box-body">
                    <div class="clearfix">
                        <div id="ranking-pagination" class="ranking-pagination"></div>
                        <div class="input-group ranking-pagination-search">
                            <input id="ranking-search-input" class="form-control"
                                   placeholder="<spring:message code="page.ranking.search"/>"/>
                            <span class="input-group-btn">
                                <button id="ranking-search-button" type="button" class="btn bg-orange"><i class="fa fa-search"></i></button>
                            </span>
                        </div>
                    </div>
                    <div id="ranking-container"></div>
                </div>
            </div>
        </div>
    </div>
</form:form>

<script type="text/javascript" src="<c:url value="/resources/js/ranking.js?v=${buildTimestamp}"/>"></script>
<script type="text/javascript">
    $(document).ready(function () {
        var ranking = new Ranking(rankingLocale);
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
                    <div id="update-level-modal-label" class="bootstrap-dialog-title" id="e3384762-1886-42af-b99a-1b37d8134022_title"></div>
                </div>
            </div>
            <div class="modal-body">
                <p><spring:message code="page.ranking.update.modal.content"/></p>
            </div>
            <div class="modal-footer">
                <div class="col-md-9">
                    <div class="row">
                        <div class="form-group col-md-6">
                            <input id="update-level-value" type="number" max="999" min="1" class="form-control"
                                   placeholder="<spring:message code="page.ranking.level.text"/>" />
                        </div>
                    </div>
                </div>
                <div class="col-md-2">
                    <button id="update-level-button" type="button" class="btn btn-warning" style="width: 110px;">
                        <span id="update-level-text">
                            <spring:message code="global.button.update"/>
                        </span>
                        <span id="update-level-spinner" style="display: none;"><i class="fa fa-circle-o-notch fa-spin" style="font-size:18px;"></i></span>
                    </button>
                </div>
            </div>
        </div>
    </div>
</div>