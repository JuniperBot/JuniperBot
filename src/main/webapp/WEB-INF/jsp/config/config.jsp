<%@ page contentType="text/html;charset=UTF-8" %>
<%@include file="/WEB-INF/template/include.jsp" %>

<spring:url value="/config/${serverId}" var="actionUrl" />

<form:form class="form-horizontal" method="post" modelAttribute="config" action="${actionUrl}">
    <div class="row">
        <div class="col-md-6">
            <div class="box box-warning">
                <div class="box-header with-border">
                    <h3 class="box-title">Основные настройки</h3>
                </div>
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

                    <div class="form-group">
                        <label class="col-sm-4 control-label combo-control">Ограничения воспроизведения</label>
                        <div class="col-sm-8">
                            <div class="row">
                                <spring:bind path="musicQueueLimit">
                                    <div class="col-sm-6 ${status.error ? 'has-error' : ''}">
                                        <label for="queue-limit" class="control-label">Треков на участника</label>
                                        <form:input id="queue-limit" type="number" min="1" path="musicQueueLimit" cssClass="form-control" placeholder="не ограничено" />
                                        <form:errors path="musicQueueLimit" class="help-block" />
                                    </div>
                                </spring:bind>
                                <spring:bind path="musicDurationLimit">
                                    <div class="col-sm-6 ${status.error ? 'has-error' : ''}">
                                        <label for="duration-limit" class="control-label">Длительность трека</label>
                                        <div class="input-group">
                                            <form:input id="duration-limit" type="number" min="1" path="musicDurationLimit" cssClass="form-control" placeholder="не ограничено" />
                                            <span class="input-group-addon">мин</span>
                                        </div>
                                        <form:errors path="musicDurationLimit" class="help-block" />
                                    </div>
                                </spring:bind>
                            </div>
                            <div class="row">
                                <spring:bind path="musicDuplicateLimit">
                                    <div class="col-sm-6 ${status.error ? 'has-error' : ''}">
                                        <label for="duplicate-limit" class="control-label">Одинаковых треков</label>
                                        <form:input id="duplicate-limit" type="number" min="1" path="musicDuplicateLimit" cssClass="form-control" placeholder="не ограничено" />
                                        <form:errors path="musicDuplicateLimit" class="help-block" />
                                    </div>
                                </spring:bind>
                                <spring:bind path="musicPlaylistEnabled">
                                    <div class="col-sm-6 ${status.error ? 'has-error' : ''}">
                                        <div class="checkbox play-checkbox">
                                            <label>
                                                <form:checkbox path="musicPlaylistEnabled" cssStyle="margin-top: 4px;" />
                                                Разрешить плейлисты
                                            </label>
                                            <form:errors path="musicPlaylistEnabled" class="help-block" />
                                        </div>
                                    </div>
                                </spring:bind>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
        </div>
        <div class="col-md-6">
            <div class="box box-warning">
                <div class="box-header with-border">
                    <h3 class="box-title">Настройки публикаций</h3>
                </div>

                <div class="box-body">
                    <spring:bind path="privateHelp">
                        <div class="form-group ${status.error ? 'has-error' : ''}">
                            <label for="input-help" class="col-sm-4 control-label">Отправлять команду <small class="label bg-yellow">хелп</small> в личку</label>
                            <div class="col-sm-8">
                                <form:checkbox id="input-help" path="privateHelp" cssClass="pull-left" cssStyle="margin-right: 5px;" />
                                <p class="help-block">(это также отключит группировку и отправит полный список команд)</p>
                                <form:errors path="privateHelp" class="help-block" />
                            </div>
                        </div>
                    </spring:bind>
                    <spring:bind path="webHook.enabled">
                        <div class="form-group ${status.error ? 'has-error' : ''}">
                            <label for="enable-publish" class="col-sm-4 control-label">Включить публикации фырок</label>
                            <div class="col-sm-8">
                                <form:checkbox id="enable-publish" disabled="${not config.webHook.available}" path="webHook.enabled" />
                                <form:errors path="webHook.enabled" class="help-block" />
                            </div>
                        </div>
                    </spring:bind>
                    <spring:bind path="webHook.channelId">
                        <div class="form-group ${status.error ? 'has-error' : ''}">
                            <label for="publish-channel" class="col-sm-4 control-label">Канал для публикаций</label>
                            <div class="col-sm-8">
                                <form:select id="publish-channel" path="webHook.channelId" disabled="${not config.webHook.available}" cssClass="form-control select2" cssStyle="width: 100%;"
                                             items="${textChannels}" itemValue="idLong" itemLabel="name" />
                                <form:errors path="webHook.channelId" class="help-block" />
                            </div>
                        </div>
                    </spring:bind>

                    <c:if test="${not config.webHook.available}">
                        <div class="callout callout-warning">
                            <p>Изменение настроек публикации недоступно, поскольку бот не добавлен на сервер и/или нет прав на управление WebHook'ами</p>
                        </div>
                    </c:if>
                </div>
            </div>
        </div>
    </div>
    <div class="row">
        <div class="col-md-6">
            <button type="submit" class="btn bg-orange">Сохранить изменения</button>
        </div>
    </div>
</form:form>