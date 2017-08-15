<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@include file="/WEB-INF/template/include.jsp" %>

<tilesx:useAttribute id="item" name="item" classname="ru.caramel.juniperbot.web.common.navigation.MenuItem" />

<li class="${empty item.childs ? '' : 'treeview'} ${item.active ? 'active' : ''}">
    <a href="<c:url value="${item.url}"/>">
        <i class="${item.icon}"></i> <span>${item.name}</span>
        <c:if test="${not empty item.childs}">
            <span class="pull-right-container">
                  <i class="fa fa-angle-left pull-right"></i>
            </span>
        </c:if>
    </a>
    <c:if test="${not empty item.childs}">
        <ul class="treeview-menu">
            <c:forEach items="${item.childs}" var="child">
                <tiles:insertDefinition name="menu.item">
                    <tiles:putAttribute name="item" value="${child}" />
                </tiles:insertDefinition>
            </c:forEach>
        </ul>
    </c:if>
</li>