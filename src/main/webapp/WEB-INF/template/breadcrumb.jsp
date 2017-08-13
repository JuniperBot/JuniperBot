<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@include file="/WEB-INF/template/include.jsp" %>

<section class="content-header">
    <h1><tiles:insertAttribute name="title" ignore="true"/>&nbsp;</h1>
    <ol class="breadcrumb">
        <li><a href="<c:url value="/"/>"><i class="fa fa-dashboard"></i> Главная</a></li>
        <%--<li><a href="#">Examples</a></li>
        <li class="active">Blank page</li>--%>
    </ol>
</section>