<%@ page contentType="text/html;charset=UTF-8" %>
<%@include file="/WEB-INF/template/include.jsp" %>

<spring:url value="/config/${serverId}" var="actionUrl" />

<div class="row">
    <div class="col-md-6">
        <div class="box box-warning">
            <div class="box-header with-border">
                <h3 class="box-title">Основные настройки</h3>
            </div>
            <form:form class="form-horizontal" method="post" modelAttribute="config" action="${actionUrl}">
                <div class="box-body">
                    <spring:bind path="prefix">
                        <div class="form-group ${status.error ? 'has-error' : ''}">
                            <label for="input-prefix" class="col-sm-2 control-label">Префикс</label>
                            <div class="col-sm-10">
                                <form:input id="input-prefix" path="prefix" type="text" class="form-control" placeholder="Префикс" />
                                <form:errors path="prefix" class="help-block" />
                            </div>
                        </div>
                    </spring:bind>
                </div>
                <div class="box-footer">
                    <button type="submit" class="btn bg-orange pull-right">Сохранить</button>
                </div>
            </form:form>
        </div>
    </div>
</div>