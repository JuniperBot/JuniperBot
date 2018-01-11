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