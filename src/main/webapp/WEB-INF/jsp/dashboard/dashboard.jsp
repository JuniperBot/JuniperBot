<%@ page contentType="text/html;charset=UTF-8" %>
<%@include file="/WEB-INF/template/include.jsp" %>

<div class="box">
    <div class="box-header with-border">
        <h3 class="box-title">Список участников и что-нибудь еще</h3>

        <div class="box-tools pull-right">
            <button type="button" class="btn btn-box-tool" data-widget="collapse" data-toggle="tooltip"
                    title="Collapse">
                <i class="fa fa-minus"></i></button>
            <button type="button" class="btn btn-box-tool" data-widget="remove" data-toggle="tooltip" title="Remove">
                <i class="fa fa-times"></i></button>
        </div>
    </div>
    <div class="box-body">Раздел пока что в разработке, но вы уже можете приступить к <a href="<c:url value="/config/${serverId}"/>">конфигурированию</a> бота на сервере.</div>
</div>