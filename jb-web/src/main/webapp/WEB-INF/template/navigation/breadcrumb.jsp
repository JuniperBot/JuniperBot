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

<section class="content-header">
    <c:if test="${breadcrumbTitleVisible}">
        <h1><tiles:insertAttribute name="title" ignore="true"/>&nbsp;</h1>
    </c:if>
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