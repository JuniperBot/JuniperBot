<!DOCTYPE HTML>
<%@include file="/WEB-INF/template/include.jsp" %>
<%@page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>

<tilesx:useAttribute id="sidebarVisible"    name="sidebarVisible"    classname="java.lang.String" ignore="true" scope="request" />
<tilesx:useAttribute id="breadcrumbVisible" name="breadcrumbVisible" classname="java.lang.String" ignore="true" scope="request" />

<html>
<head>
    <meta charset="utf-8">
    <meta http-equiv="X-UA-Compatible" content="IE=edge">
    <title><tiles:insertAttribute name="title" ignore="true"/></title>

    <meta content="width=device-width, initial-scale=1, maximum-scale=1, user-scalable=no" name="viewport">
    <link rel="stylesheet" href="<c:url value="/resources/css/bootstrap.min.css"/>">
    <link rel="stylesheet" href="<c:url value="/resources/css/font-awesome.min.css"/>">
    <link rel="stylesheet" href="<c:url value="/resources/css/ionicons.min.css"/>">
    <link rel="stylesheet" href="<c:url value="/resources/css/AdminLTE.min.css"/>">
    <link rel="stylesheet" href="<c:url value="/resources/css/skin-yellow-light.min.css"/>">
    <link rel="stylesheet" href="<c:url value="/resources/css/main.css"/>">

    <!-- HTML5 Shim and Respond.js IE8 support of HTML5 elements and media queries -->
    <!-- WARNING: Respond.js doesn't work if you view the page via file:// -->
    <!--[if lt IE 9]>
    <script src="https://oss.maxcdn.com/html5shiv/3.7.3/html5shiv.min.js"></script>
    <script src="https://oss.maxcdn.com/respond/1.4.2/respond.min.js"></script>
    <![endif]-->

    <link rel="stylesheet" href="https://fonts.googleapis.com/css?family=Source+Sans+Pro:300,400,600,700,300italic,400italic,600italic">
</head>
<body class="hold-transition sidebar-mini skin-yellow-light <tiles:insertAttribute name="body-class" ignore="true" />">
    <div class="wrapper">
        <jsp:include page="header.jsp" />
        <c:if test="${sidebarVisible}">
            <jsp:include page="sidebar.jsp" />
        </c:if>
        <div class="content-wrapper <c:if test="${not sidebarVisible}">no-sidebar</c:if>">
            <c:if test="${breadcrumbVisible}">
                <jsp:include page="breadcrumb.jsp" />
            </c:if>
            <section class="content">
                <tiles:insertAttribute name="content" />
            </section>
        </div>
        <jsp:include page="footer.jsp" />
    </div>
    <script type="text/javascript" src="<c:url value="/resources/js/jquery.min.js"/>"></script>
    <script type="text/javascript" src="<c:url value="/resources/js/bootstrap.min.js"/>"></script>
    <script type="text/javascript" src="<c:url value="/resources/js/adminlte.min.js"/>"></script>
    <script type="text/javascript" src="<c:url value="/resources/js/script.js"/>"></script>
    <c:if test="${sidebarVisible}">
        <script>
            $(document).ready(function () {
                $('.sidebar-menu').tree()
            })
        </script>
    </c:if>
</body>
</html>