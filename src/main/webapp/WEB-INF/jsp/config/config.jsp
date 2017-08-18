<%@ page contentType="text/html;charset=UTF-8" %>
<%@include file="/WEB-INF/template/include.jsp" %>

<spring:url value="/config/${serverId}" var="actionUrl" />

<div class="row">
    <div class="col-md-7">
        <div class="box box-warning">
            <div class="box-header with-border">
                <h3 class="box-title">Основные настройки</h3>
            </div>
            <form:form class="form-horizontal" method="post" modelAttribute="config" action="${actionUrl}">
                <div class="box-body">
                    <spring:bind path="prefix">
                        <div class="form-group ${status.error ? 'has-error' : ''}">
                            <label for="input-prefix" class="col-sm-4 control-label">Префикс</label>
                            <div class="col-sm-8">
                                <form:input id="input-prefix" path="prefix" type="text" class="form-control" placeholder="Префикс" />
                                <form:errors path="prefix" class="help-block" />
                            </div>
                        </div>
                    </spring:bind>
                    <hr />
                    <spring:bind path="musicChannelId">
                        <div class="form-group ${status.error ? 'has-error' : ''}">
                            <label for="music-channel" class="col-sm-4 control-label">Музыкальный канал по-умолчанию</label>
                            <div class="col-sm-8">
                                <form:select id="music-channel" path="musicChannelId" disabled="${not serverAdded}" cssClass="form-control select2" cssStyle="width: 100%;"
                                             items="${voiceChannels}" itemValue="idLong" itemLabel="name" />
                                <form:errors path="musicChannelId" class="help-block" />
                            </div>
                        </div>
                    </spring:bind>
                    <spring:bind path="musicPlaylistEnabled">
                        <div class="form-group ${status.error ? 'has-error' : ''}">
                            <label for="playlist-enabled" class="col-sm-4 control-label">Разрешить плейлисты</label>
                            <div class="col-sm-8">
                                <form:checkbox id="playlist-enabled" path="musicPlaylistEnabled" />
                                <form:errors path="musicPlaylistEnabled" class="help-block" />
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