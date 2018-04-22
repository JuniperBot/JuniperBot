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

<spring:url value="/funny/${serverId}" var="actionUrl"/>

<spring:message code="global.switch.on" var="switchOn"/>
<spring:message code="global.switch.off" var="switchOff"/>

<form:form class="form-horizontal" method="post" modelAttribute="funny" action="${actionUrl}">
    <div class="row">
        <div class="col-lg-6">
            <div class="box box-warning">
                <div class="box-header with-border">
                    <h3 class="box-title"><spring:message code="page.funny.reactionRoulette.title"/>
                        <i class="fa fa-fw fa-question-circle" data-toggle="tooltip"
                           title="<spring:message code="page.funny.reactionRoulette.help"/>"
                           data-container="body"></i></h3>
                    <div class="box-tools pull-right">
                        <form:checkbox path="reactionRoulette.enabled"
                                       data-toggle="toggle"
                                       data-onstyle="warning"
                                       data-size="small"
                                       data-on="${switchOn}"
                                       data-off="${switchOff}"/>
                    </div>
                </div>
                <div class="box-body">
                    <spring:bind path="reactionRoulette.reaction">
                        <div class="form-group checkbox-group ${status.error ? 'has-error' : ''}">
                            <label for="as-reaction" class="col-sm-5 control-label">
                                <spring:message code="page.funny.reactionRoulette.reaction"/>
                            </label>
                            <div class="col-sm-7">
                                <form:checkbox id="as-reaction" path="reactionRoulette.reaction"
                                               data-toggle="toggle"
                                               data-onstyle="warning"
                                               data-size="small"
                                               data-on="${switchOn}"
                                               data-off="${switchOff}"/>
                                <form:errors path="reactionRoulette.reaction" class="help-block"/>
                            </div>
                        </div>
                    </spring:bind>

                    <spring:bind path="reactionRoulette.percent">
                        <div class="form-group checkbox-group ${status.error ? 'has-error' : ''}">
                            <label for="percent" class="col-sm-5 control-label">
                                <spring:message code="page.funny.reactionRoulette.percent"/>
                            </label>
                            <div class="col-sm-7">
                                <spring:message code="page.funny.reactionRoulette.percent.placeholder"
                                                var="percentPlaceholder"/>
                                <div class="input-group">
                                    <form:input id="percent" type="number" min="1" max="5"
                                                path="reactionRoulette.percent"
                                                cssClass="form-control" placeholder="${percentPlaceholder}"/>
                                    <span class="input-group-addon">%</span>
                                </div>
                            </div>
                        </div>
                    </spring:bind>
                </div>
            </div>
        </div>
    </div>
    <div class="row">
        <div class="col-md-6">
            <button type="submit" class="btn bg-orange"><spring:message code="global.button.save"/></button>
        </div>
    </div>
</form:form>