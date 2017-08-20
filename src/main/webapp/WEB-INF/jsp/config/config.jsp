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
                    <spring:bind path="privateHelp">
                        <div class="form-group ${status.error ? 'has-error' : ''}">
                            <label for="input-help" class="col-sm-4 control-label">Отправлять команду <small class="label bg-yellow">хелп</small> в личку</label>
                            <div class="col-sm-8">
                                <form:checkbox id="input-help" path="privateHelp" cssClass="pull-left" cssStyle="margin-right: 5px;" />
                                <p class="help-block">(это так же отключит группировку и отправит полный список команд)</p>
                                <form:errors path="privateHelp" class="help-block" />
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
                <div class="box-footer">
                    <button type="submit" class="btn bg-orange pull-right">Сохранить</button>
                </div>
            </form:form>
        </div>
    </div>
</div>