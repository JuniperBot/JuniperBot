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
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@include file="/WEB-INF/template/include.jsp" %>

<c:forEach var="entry" items="${flash}">
    <c:choose>
        <c:when test="${entry.type == 'INFO'}">
            <c:set var="alertClass" value="alert-info" />
            <c:set var="iconClass" value="fa fa-info" />
            <c:set var="label" value="flash.caption.info" />
        </c:when>
        <c:when test="${entry.type == 'ERROR'}">
            <c:set var="alertClass" value="alert-danger" />
            <c:set var="iconClass" value="fa fa-ban" />
            <c:set var="label" value="flash.caption.error" />
        </c:when>
        <c:when test="${entry.type == 'WARNING'}">
            <c:set var="alertClass" value="alert-warning" />
            <c:set var="iconClass" value="fa fa-warning" />
            <c:set var="label" value="flash.caption.warn" />
        </c:when>
        <c:when test="${entry.type == 'SUCCESS'}">
            <c:set var="alertClass" value="alert-success" />
            <c:set var="iconClass" value="fa fa-check" />
            <c:set var="label" value="flash.caption.success" />
        </c:when>
    </c:choose>
    <div id="flash-${entry.key}" class="alert ${alertClass} alert-dismissible flash-message">
        <button type="button" class="close" data-dismiss="alert" aria-hidden="true">×</button>
        <h4><i class="icon ${iconClass}"></i> <spring:message code="${label}" /></h4>
        <spring:message message="${entry.resolvable}" />
    </div>
</c:forEach>