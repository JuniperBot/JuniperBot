<!DOCTYPE HTML>
<%@include file="/WEB-INF/template/include.jsp" %>
<%@page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>

<html>
<head>
    <title><tiles:insertAttribute name="title" ignore="true"/></title>
    <link rel="icon" href="<c:url value="/resources/img/favicon.ico"/>" type="image/x-icon" />
    <link rel="shortcut icon" href="<c:url value="/resources/img/favicon.ico"/>" type="image/x-icon" />
    <meta charset="utf-8">

    <meta name="viewport" content="width=device-width, initial-scale=1.0">

</head>
<body>
<div id="page">
    <tiles:insertAttribute name="content" />
</div>
</body>
</html>