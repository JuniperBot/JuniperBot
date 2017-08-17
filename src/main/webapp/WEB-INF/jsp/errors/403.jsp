<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@include file="/WEB-INF/template/include.jsp" %>

<div class="error-page text-white">
    <h2 class="headline">403</h2>
    <div class="error-content">
        <h3><i class="fa fa-search"></i> Урф... Доступ запрещен.</h3>
        <p>
            Доступ к запрашиваемой странице запрещен.
            Вы можете <a href="<c:url value="/"/>">вернуться на главную</a> и попробовать что-нибудь еще.
        </p>
    </div>
</div>