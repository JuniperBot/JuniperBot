<%@ page contentType="text/html;charset=UTF-8" %>
<%@include file="/WEB-INF/template/include.jsp" %>

<aside class="main-sidebar">
    <section class="sidebar">
        <ul class="sidebar-menu" data-widget="tree">
            <li><a href="<c:url value="/dashboard"/>"><i class="fa fa-dashboard"></i> <span>Мониторинг</span></a></li>
            <li><a href="<c:url value="/config"/>"><i class="fa fa-cogs"></i> <span>Конфигурация</span></a></li>
        </ul>
    </section>
</aside>