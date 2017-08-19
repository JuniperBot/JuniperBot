<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@include file="/WEB-INF/template/include.jsp" %>

<section class="content-header">
    <h1><tiles:insertAttribute name="title" ignore="true"/>&nbsp;</h1>
    <ol class="breadcrumb">
        <c:forEach var="entry" items="${breadCrumb}">
            <c:choose>
                <c:when test="${entry.current}">
                    <li class="active">
                        <c:if test="${not empty entry.icon}">
                            <span><i class="${entry.icon}"></i>&nbsp;</span>
                        </c:if>
                        <span>${entry.name}</span>
                    </li>
                </c:when>
                <c:otherwise>
                    <li>
                        <a href="<c:url value="${entry.url}"/>">
                            <c:if test="${not empty entry.icon}">
                                <span><i class="${entry.icon}"></i>&nbsp;</span>
                            </c:if>
                            <span>${entry.name}</span>
                        </a>
                    </li>
                </c:otherwise>
            </c:choose>
        </c:forEach>
    </ol>
</section>