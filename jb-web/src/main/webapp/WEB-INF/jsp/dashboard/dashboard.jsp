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

<c:url value="/config/${serverId}" var="configUrl"/>

<div class="box">
    <div class="box-header with-border">
        <h3 class="box-title"><spring:message code="page.dashboard.title"/></h3>

        <div class="box-tools pull-right">
            <button type="button" class="btn btn-box-tool" data-widget="collapse" data-toggle="tooltip"
                    title="<spring:message code="global.title.collapse"/>">
                <i class="fa fa-minus"></i></button>
            <button type="button" class="btn btn-box-tool" data-widget="remove" data-toggle="tooltip"
                    title="<spring:message code="global.title.remove"/>">
                <i class="fa fa-times"></i></button>
        </div>
    </div>
    <div class="box-body"><spring:message code="page.dashboard.content" arguments="${configUrl}"/></div>
</div>