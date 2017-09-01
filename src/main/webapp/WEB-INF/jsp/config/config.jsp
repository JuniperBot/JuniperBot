<%@ page contentType="text/html;charset=UTF-8" %>
<%@include file="/WEB-INF/template/include.jsp" %>

<spring:url value="/config/${serverId}" var="actionUrl" />

<div id="vk-connect-modal" class="modal fade" tabindex="-1" role="dialog" aria-labelledby="vk-connect-modal-label">
    <div class="modal-dialog" role="document">
        <div class="modal-content">
            <div class="modal-header">
                <button type="button" class="close" data-dismiss="modal" aria-label="Close"><span aria-hidden="true">&times;</span></button>
                <h4 class="modal-title" id="vk-connect-modal-label">Подключение сообщества ВКонтакте</h4>
            </div>
            <div class="modal-body">
                <div id="vk-first-step">
                    <p>Для подключения своего сообщества к боту необходимо сделать следующее:</p>
                    <ol>
                        <li>Зайти в управление сообществом на его странице ВКонтакте;</li>
                        <li>Открыть раздел <b>Работа с API</b>;</li>
                        <li>Перейти во вкладку <b>Callback API</b>;</li>
                        <li>Добавить новый сервер с помощью кнопки <b>Добавить сервер</b> или воспользоваться имеющимся, если он не используется;</li>
                        <li>Во вкладке <b>Типы событий</b> найти раздел <b>Записи на стене</b> и поставить галочку напротив <b>Добавление</b>;</li>
                        <li>Во вкладке <b>Настройки сервера</b> скопировать выделенный жирным код подтверждения сервера (строка, которую должен вернуть сервер) в поле ниже, ввести название и нажать оранжевую кнопочку <b>Подключить</b>.</li>
                    </ol>
                    <div class="callout">
                        <p>Не закрывайте страницу ВКонтакте откуда вы скопировали этот код! Она вам еще понадобится.</p>
                    </div>
                </div>
                <div id="vk-second-step">
                    <p>Отлично! Мы готовы принимать запросы от ВКонтакте! Но нужно сделать кое-что еще:</p>
                    <ol>
                        <li>Вернитесь на страницу, откуда вы брали код подтверждения;</li>
                        <li>Введите там в поле <b>Адрес</b>, указанный ниже и нажмите там же кнопку <b>Подтвердить</b>;</li>
                        <li>Готово! Через некоторое время мы установим соединение с ВКонтакте и вы сможете выбрать канал, в который будут публиковаться ваши фыр-фырные посты.</li>
                    </ol>
                    <div class="form-group">
                        <div class="input-group">
                            <input id="vk-address-input" type="text" class="form-control" readonly>
                            <span class="input-group-btn">
                                <button id="vk-address-copy" type="button" class="btn" title="Скопировать в буфер обмена"
                                        data-toggle="tooltip" data-placement="bottom"><i class="fa fa-copy"></i></button>
                            </span>
                        </div>
                    </div>
                </div>
            </div>
            <div class="modal-footer">
                <div class="col-md-9">
                    <div class="row">
                        <div class="form-group col-md-6">
                            <input id="vk-connection-name" class="form-control" maxlength="255" placeholder="Название сообщества" />
                        </div>
                        <div class="form-group col-md-6">
                            <input id="vk-confirmation-code" class="form-control" maxlength="50" placeholder="Код подтверждения" />
                        </div>
                    </div>
                </div>
                <div class="col-md-2">
                    <button id="vk-create-button" type="button" class="btn btn-warning" style="width: 110px;">
                        <span id="vk-connect-text">Подключить</span>
                        <span id="vk-connect-spinner" style="display: none;"><i class="fa fa-circle-o-notch fa-spin" style="font-size:18px;"></i></span>
                    </button>
                </div>
            </div>
        </div>
    </div>
</div>

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
                    <spring:bind path="musicConfig.channelId">
                        <div class="form-group ${status.error ? 'has-error' : ''}">
                            <label for="music-channel" class="col-sm-4 control-label">Музыкальный канал по-умолчанию</label>
                            <div class="col-sm-8">
                                <form:select id="music-channel" path="musicConfig.channelId" disabled="${not serverAdded}" cssClass="form-control select2" cssStyle="width: 100%;"
                                             items="${voiceChannels}" itemValue="idLong" itemLabel="name" />
                                <form:errors path="musicConfig.channelId" class="help-block" />
                                <spring:bind path="musicConfig.userJoinEnabled">
                                    <div class="checkbox ${status.error ? 'has-error' : ''}" style="padding-top: 0px;">
                                        <label>
                                            <form:checkbox path="musicConfig.userJoinEnabled" cssStyle="margin-top: 4px;" />
                                            Разрешить присоединяться в канал пользователя
                                        </label>
                                        <form:errors path="musicConfig.userJoinEnabled" class="help-block" />
                                    </div>
                                </spring:bind>
                            </div>
                        </div>
                    </spring:bind>

                    <div class="form-group">
                        <label class="col-sm-4 control-label combo-control">Ограничения воспроизведения</label>
                        <div class="col-sm-8">
                            <div class="row">
                                <spring:bind path="musicConfig.queueLimit">
                                    <div class="col-sm-6 ${status.error ? 'has-error' : ''}">
                                        <label for="queue-limit" class="control-label">Треков на участника</label>
                                        <form:input id="queue-limit" type="number" min="1" path="musicConfig.queueLimit" cssClass="form-control" placeholder="не ограничено" />
                                        <form:errors path="musicConfig.queueLimit" class="help-block" />
                                    </div>
                                </spring:bind>
                                <spring:bind path="musicConfig.durationLimit">
                                    <div class="col-sm-6 ${status.error ? 'has-error' : ''}">
                                        <label for="duration-limit" class="control-label">Длительность трека</label>
                                        <div class="input-group">
                                            <form:input id="duration-limit" type="number" min="1" path="musicConfig.durationLimit" cssClass="form-control" placeholder="не ограничено" />
                                            <span class="input-group-addon">мин</span>
                                        </div>
                                        <form:errors path="musicConfig.durationLimit" class="help-block" />
                                    </div>
                                </spring:bind>
                            </div>
                            <div class="row">
                                <spring:bind path="musicConfig.duplicateLimit">
                                    <div class="col-sm-6 ${status.error ? 'has-error' : ''}">
                                        <label for="duplicate-limit" class="control-label">Одинаковых треков</label>
                                        <form:input id="duplicate-limit" type="number" min="1" path="musicConfig.duplicateLimit" cssClass="form-control" placeholder="не ограничено" />
                                        <form:errors path="musicConfig.duplicateLimit" class="help-block" />
                                    </div>
                                </spring:bind>
                                <div class="col-sm-6 ${status.error ? 'has-error' : ''}">
                                    <spring:bind path="musicConfig.playlistEnabled">
                                        <div class="checkbox play-checkbox ${status.error ? 'has-error' : ''}">
                                            <label>
                                                <form:checkbox path="musicConfig.playlistEnabled" cssStyle="margin-top: 4px;" />
                                                Разрешить плейлисты
                                            </label>
                                            <form:errors path="musicConfig.playlistEnabled" class="help-block" />
                                        </div>
                                    </spring:bind>
                                    <spring:bind path="musicConfig.streamsEnabled">
                                        <div class="checkbox ${status.error ? 'has-error' : ''}" style="padding-top: 0px;">
                                            <label>
                                                <form:checkbox path="musicConfig.streamsEnabled" cssStyle="margin-top: 4px;" />
                                                Разрешить потоковое аудио
                                            </label>
                                            <form:errors path="musicConfig.streamsEnabled" class="help-block" />
                                        </div>
                                    </spring:bind>
                                </div>
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
                    <form:hidden path="webHook.available" />
                    <spring:bind path="privateHelp">
                        <div class="form-group checkbox-group ${status.error ? 'has-error' : ''}">
                            <label for="input-help" class="col-sm-4 control-label">Отправлять команду <small class="label bg-yellow">хелп</small> в личку</label>
                            <div class="col-sm-8">
                                <form:checkbox id="input-help" path="privateHelp" cssClass="pull-left" cssStyle="margin-right: 5px;" />
                                <p class="help-block">(это также отключит группировку и отправит полный список команд)</p>
                                <form:errors path="privateHelp" class="help-block" />
                            </div>
                        </div>
                    </spring:bind>

                    <spring:bind path="webHook.channelId">
                        <div class="form-group ${status.error ? 'has-error' : ''}">
                            <label for="publish-channel" class="col-sm-4 control-label">Публикация фыров от Джупи</label>
                            <div class="col-sm-8">
                                <div class="input-group">
                                    <span class="input-group-addon">
                                        <form:checkbox disabled="${not config.webHook.available}" path="webHook.enabled" />
                                    </span>
                                    <form:select id="publish-channel" path="webHook.channelId" disabled="${not config.webHook.available}" cssClass="form-control select2" cssStyle="width: 100%;"
                                                 items="${textChannels}" itemValue="idLong" itemLabel="name" />
                                    <form:errors path="webHook.channelId" class="help-block" />
                                </div>
                            </div>
                        </div>
                    </spring:bind>

                    <div id="vk-connection-list">
                        <c:forEach items="${config.vkConnections}" var="vkConnection" varStatus="status">
                            <form:hidden path="vkConnections[${status.index}].id" />
                            <form:hidden path="vkConnections[${status.index}].webHook.available" />
                            <div class="form-group">
                                <label for="vk-connection-${status.index}" class="col-sm-4 control-label"><i class="fa fa-vk"></i> <c:out value="${vkConnection.name}"/></label>
                                <div class="col-sm-8">
                                    <div class="input-group">
                                        <c:if test="${config.vkConnections[status.index].status == 'CONNECTED'}">
                                            <span class="input-group-addon">
                                                <form:checkbox path="vkConnections[${status.index}].webHook.enabled"
                                                               disabled="${not config.vkConnections[status.index].webHook.available}" />
                                            </span>
                                            <form:select id="vk-connection-${status.index}"
                                                         path="vkConnections[${status.index}].webHook.channelId" disabled="${not config.vkConnections[status.index].webHook.available}"
                                                         cssClass="form-control select2" cssStyle="width: 100%;"
                                                         items="${textChannels}" itemValue="idLong" itemLabel="name" />
                                            <form:errors path="vkConnections[${status.index}].webHook.channelId" class="help-block" />
                                        </c:if>
                                        <c:if test="${config.vkConnections[status.index].status == 'CONFIRMATION'}">
                                            <input id="vk-connection-${status.index}" type="text" value="Ожидается подтверждение..." class="form-control" disabled>
                                        </c:if>
                                        <span class="input-group-btn">
                                        <button type="button" class="btn btn-danger btn-flat vk-remove-btn"
                                                data-vk-id="${config.vkConnections[status.index].id}"
                                                data-vk-name="${config.vkConnections[status.index].name}">
                                            <i class="fa fa-remove"></i>
                                        </button>
                                    </span>
                                    </div>
                                </div>
                            </div>
                        </c:forEach>
                    </div>

                    <a id="vk-connect-button" class="btn btn-block btn-social btn-vk" ${config.webHook.available ? '' : 'disabled'}>
                        <i class="fa fa-vk" style="margin-top: -2px;"></i> Подключить сообщество ВКонтакте
                    </a>

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

<script type="text/javascript" src="<c:url value="/resources/js/vk-connector.js?v=${buildTimestamp}"/>"></script>
<script type="text/javascript">
    $(document).ready(function () {
        var connector = new VkConnector();
        connector.init();
    });
</script>