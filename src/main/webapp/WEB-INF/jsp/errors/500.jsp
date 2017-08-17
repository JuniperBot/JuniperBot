<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@include file="/WEB-INF/template/include.jsp" %>

<div class="error-page text-white">
    <h2 class="headline">500</h2>
    <div class="error-content">
        <h3><i class="fa fa-search"></i> Аурф! Что-то пошло не так.</h3>
        <p>
            Произошла какая-то внутренняя ошибка сервера.
            Вы можете <a href="<c:url value="/"/>">вернуться на главную</a> или повторить попытку позже.
        </p>
    </div>
</div>