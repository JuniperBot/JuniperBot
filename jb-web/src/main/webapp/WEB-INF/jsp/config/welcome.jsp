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

<spring:url value="/welcome/${serverId}" var="actionUrl" />

<spring:message code="global.switch.on" var="switchOn"/>
<spring:message code="global.switch.off" var="switchOff"/>

<form:form class="form-horizontal" method="post" modelAttribute="welcomeMessage" action="${actionUrl}">
    <div class="row">
        <div class="col-md-12">
            <div class="callout callout-warning">
                <h4><spring:message code="global.title.hint"/></h4>
                <p><spring:message code="page.welcome.hint.1"/></p>
                <p><spring:message code="page.welcome.hint.2"/></p>
            </div>
        </div>
    </div>
    <div class="row">
        <div class="col-md-6">
            <div class="box box-warning">
                <div class="box-header with-border">
                    <h3 class="box-title"><spring:message code="page.welcome.join.title"/></h3>
                    <div class="box-tools pull-right">
                        <form:checkbox path="joinEnabled"
                                       data-toggle="toggle"
                                       data-onstyle="warning"
                                       data-size="small"
                                       data-on="${switchOn}"
                                       data-off="${switchOff}" />
                    </div>
                </div>
                <div class="box-body">
                    <spring:bind path="joinMessage">
                        <div class="form-group ${status.error ? 'has-error' : ''}">
                            <div class="col-md-12">
                                <spring:message code="page.welcome.join.placeholder" var="joinPlaceholder"/>
                                <form:textarea path="joinMessage" cssClass="form-control" rows="5" placeholder="${joinPlaceholder}" />
                                <form:errors path="joinMessage" class="help-block" />
                            </div>
                        </div>
                    </spring:bind>

                    <div class="form-group">
                        <label for="joinRichEnabled" class="col-sm-4 control-label">
                            <spring:message code="page.welcome.rich"/>
                        </label>
                        <div class="col-sm-2">
                            <form:checkbox id="joinRichEnabled" path="joinRichEnabled"
                                           data-toggle="toggle"
                                           data-onstyle="warning"
                                           data-size="small"
                                           data-on="${switchOn}"
                                           data-off="${switchOff}" />
                        </div>
                        <div class="col-sm-6">
                            <div class="row">
                                <label for="joinToDM" class="col-sm-6 control-label">
                                    <spring:message code="global.button.sendToDM"/>
                                </label>
                                <div class="col-sm-6">
                                    <form:checkbox id="joinToDM" path="joinToDM"
                                                   data-toggle="toggle"
                                                   data-onstyle="warning"
                                                   data-size="small"
                                                   data-on="${switchOn}"
                                                   data-off="${switchOff}" />
                                </div>
                            </div>
                        </div>
                    </div>

                    <spring:bind path="joinChannelId">
                        <div class="form-group ${status.error ? 'has-error' : ''}">
                            <label for="joinChannelId" class="col-sm-4 control-label">
                                <spring:message code="page.welcome.channel"/>
                            </label>
                            <div class="col-sm-8">
                                <form:select id="joinChannelId" path="joinChannelId" disabled="${not serverAdded}" cssClass="form-control select2" cssStyle="width: 100%;"
                                             items="${textChannels}" itemValue="idLong" itemLabel="name" />
                                <form:errors path="joinChannelId" class="help-block" />
                            </div>
                        </div>
                    </spring:bind>

                </div>
            </div>
        </div>
        <div class="col-md-6">
            <div class="box box-warning">
                <div class="box-header with-border">
                    <h3 class="box-title">
                        <spring:message code="page.welcome.leave.title"/>
                    </h3>
                    <div class="box-tools pull-right">
                        <form:checkbox path="leaveEnabled"
                                       data-toggle="toggle"
                                       data-onstyle="warning"
                                       data-size="small"
                                       data-on="${switchOn}"
                                       data-off="${switchOff}" />
                    </div>
                </div>

                <div class="box-body">
                    <spring:bind path="leaveMessage">
                        <div class="form-group ${status.error ? 'has-error' : ''}">
                            <div class="col-md-12">
                                <spring:message code="page.welcome.leave.placeholder" var="leavePlaceholder"/>
                                <form:textarea path="leaveMessage" cssClass="form-control" rows="5"
                                               placeholder="${leavePlaceholder}" />
                                <form:errors path="leaveMessage" class="help-block" />
                            </div>
                        </div>
                    </spring:bind>

                    <div class="form-group">
                        <label for="leaveRichEnabled" class="col-sm-4 control-label">
                            <spring:message code="page.welcome.rich"/>
                        </label>
                        <div class="col-sm-8">
                            <form:checkbox id="leaveRichEnabled" path="leaveRichEnabled"
                                           data-toggle="toggle"
                                           data-onstyle="warning"
                                           data-size="small"
                                           data-on="${switchOn}"
                                           data-off="${switchOff}" />
                        </div>
                    </div>

                    <spring:bind path="leaveChannelId">
                        <div class="form-group ${status.error ? 'has-error' : ''}">
                            <label for="leaveChannelId" class="col-sm-4 control-label">
                                <spring:message code="page.welcome.channel"/>
                            </label>
                            <div class="col-sm-8">
                                <form:select id="leaveChannelId" path="leaveChannelId" disabled="${not serverAdded}" cssClass="form-control select2" cssStyle="width: 100%;"
                                             items="${textChannels}" itemValue="idLong" itemLabel="name" />
                                <form:errors path="leaveChannelId" class="help-block" />
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