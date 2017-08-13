<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ page contentType="text/html;charset=UTF-8" %>

<div class="container">
    <div class="row server-select">
        <div class="col-md-6 col-md-offset-3 text-center">
            <h3>Выберите <img src="<c:out value="/resources/img/discord-logo-black.svg"/>" class="discord-logo"> сервер:</h3>
        </div>
    </div>

    <div class="row row-centered">
        <c:forEach var="server" items="${servers}">
            <div class="col-md-2 text-center col-centered">
                <a href="<c:out value="/dashboard" />/${server.id}">
                    <div class="server-item-wrapper">
                        <img src="${server.avatarUrl}" class="img-circle server-item" title="${server.name}" data-toggle="tooltip" data-placement="bottom" />
                    </div>
                </a>
            </div>
        </c:forEach>
    </div>
</div>
