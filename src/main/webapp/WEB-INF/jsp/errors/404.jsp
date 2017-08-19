<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@include file="/WEB-INF/template/include.jsp" %>

<div class="error-page text-white">
    <h2 class="headline">404</h2>
    <div class="error-content">
        <h3><i class="fa fa-search"></i> *Нюх-нюх* Страница не найдена.</h3>
        <p>
            К сожалению, мы не смогли найти запрошенную страницу.
            Вы можете <a href="<c:url value="/"/>">вернуться на главную</a> и найти самостоятельно то, что Вам нужно
        </p>
    </div>
</div>