<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@include file="/WEB-INF/template/include.jsp" %>

<c:forEach var="entry" items="${flash}">
    <c:choose>
        <c:when test="${entry.type == 'INFO'}">
            <c:set var="alertClass" value="alert-info" />
            <c:set var="iconClass" value="fa fa-info" />
            <c:set var="label" value="Информация" />
        </c:when>
        <c:when test="${entry.type == 'ERROR'}">
            <c:set var="alertClass" value="alert-danger" />
            <c:set var="iconClass" value="fa fa-ban" />
            <c:set var="label" value="Ошибка!" />
        </c:when>
        <c:when test="${entry.type == 'WARNING'}">
            <c:set var="alertClass" value="alert-warning" />
            <c:set var="iconClass" value="fa fa-warning" />
            <c:set var="label" value="Внимание!" />
        </c:when>
        <c:when test="${entry.type == 'SUCCESS'}">
            <c:set var="alertClass" value="alert-success" />
            <c:set var="iconClass" value="fa fa-check" />
            <c:set var="label" value="Выполнено!" />
        </c:when>
    </c:choose>
    <div id="flash-${entry.key}" class="alert ${alertClass} alert-dismissible flash-message">
        <button type="button" class="close" data-dismiss="alert" aria-hidden="true">×</button>
        <h4><i class="icon ${iconClass}"></i> ${label}</h4>
        <spring:message message="${entry.resolvable}" />
    </div>
</c:forEach>