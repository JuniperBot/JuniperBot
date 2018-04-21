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
<%@ page contentType="text/html;charset=UTF-8" %>
<%@include file="/WEB-INF/template/include.jsp" %>

<spring:url value="/config/${serverId}" var="actionUrl" />

<spring:message code="global.switch.on" var="switchOn"/>
<spring:message code="global.switch.off" var="switchOff"/>

<div id="vk-connect-modal" class="modal fade" tabindex="-1" role="dialog" aria-labelledby="vk-connect-modal-label">
    <div class="modal-dialog" role="document">
        <div class="modal-content">
            <div class="modal-header">
                <button type="button" class="close" data-dismiss="modal" aria-label="Close"><span aria-hidden="true">&times;</span></button>
                <h4 class="modal-title" id="vk-connect-modal-label"><spring:message code="page.config.vk.modal.title"/></h4>
            </div>
            <div class="modal-body">
                <div id="vk-first-step">
                    <p><spring:message code="page.config.vk.modal.step1.workflow.intro"/></p>
                    <ol>
                        <li><spring:message code="page.config.vk.modal.step1.workflow.list.1"/></li>
                        <li><spring:message code="page.config.vk.modal.step1.workflow.list.2"/></li>
                        <li><spring:message code="page.config.vk.modal.step1.workflow.list.3"/></li>
                        <li><spring:message code="page.config.vk.modal.step1.workflow.list.4"/></li>
                        <li><spring:message code="page.config.vk.modal.step1.workflow.list.5"/></li>
                        <li><spring:message code="page.config.vk.modal.step1.workflow.list.6"/></li>
                    </ol>
                    <div class="callout">
                        <p><spring:message code="page.config.vk.modal.step1.workflow.callout"/></p>
                    </div>
                </div>
                <div id="vk-second-step">
                    <p><spring:message code="page.config.vk.modal.step2.workflow.intro"/></p>
                    <ol>
                        <li><spring:message code="page.config.vk.modal.step2.workflow.list.1"/></li>
                        <li><spring:message code="page.config.vk.modal.step2.workflow.list.2"/></li>
                        <li><spring:message code="page.config.vk.modal.step2.workflow.list.3"/></li>
                    </ol>
                    <div class="form-group">
                        <div class="input-group">
                            <input id="vk-address-input" type="text" class="form-control" readonly>
                            <span class="input-group-btn">
                                <button id="vk-address-copy" type="button" class="btn"
                                        title="<spring:message code="global.button.clipboard"/>"
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
                            <input id="vk-connection-name" class="form-control" maxlength="255"
                                   placeholder="<spring:message code="page.config.vk.modal.step2.connection-name.placeholder"/>" />
                        </div>
                        <div class="form-group col-md-6">
                            <input id="vk-confirmation-code" class="form-control" maxlength="50"
                                   placeholder="<spring:message code="page.config.vk.modal.step2.confirmation-code.placeholder"/>" />
                        </div>
                    </div>
                </div>
                <div class="col-md-2">
                    <button id="vk-create-button" type="button" class="btn btn-warning" style="width: 110px;">
                        <span id="vk-connect-text"><spring:message code="page.config.vk.modal.buttons.connect"/></span>
                        <span id="vk-connect-spinner" style="display: none;"><i class="fa fa-circle-o-notch fa-spin" style="font-size:18px;"></i></span>
                    </button>
                </div>
            </div>
        </div>
    </div>
</div>

<form:form class="form-horizontal" method="post" modelAttribute="config" action="${actionUrl}">
    <div class="row">
        <div class="col-lg-8 col-md-11">
            <div class="nav-tabs-custom nav-tabs-warn">
                <ul id="config-tabs" class="nav nav-tabs">
                    <li class="active"><a href="#tab_common_config" data-toggle="tab" aria-expanded="true"><spring:message code="page.config.common.title"/>
                        <i class="error-mark hide fa fa-fw fa-exclamation-triangle text-red"></i></a></li>
                    <li><a href="#tab_music_config" data-toggle="tab" aria-expanded="false"><spring:message code="page.config.music.title"/>
                        <i class="error-mark hide fa fa-fw fa-exclamation-triangle text-red"></i></a></li>
                    <li><a href="#tab_publish" data-toggle="tab"><spring:message code="page.config.publish.title"/>
                        <i class="error-mark hide fa fa-fw fa-exclamation-triangle text-red"></i></a></li>
                </ul>
                <div class="tab-content">
                    <div class="tab-pane active" id="tab_common_config">
                        <spring:bind path="prefix">
                            <div class="form-group ${status.error ? 'has-error' : ''}">
                                <spring:message code="page.config.common.prefix" var="prefixLocale"/>
                                <label for="input-prefix" class="col-sm-5 control-label">${prefixLocale}</label>
                                <div class="col-sm-7">
                                    <form:input id="input-prefix" path="prefix" type="text" class="form-control"
                                                placeholder="${prefixLocale}" />
                                    <form:errors path="prefix" class="help-block" />
                                </div>
                            </div>
                        </spring:bind>
                        <spring:bind path="locale">
                            <div class="form-group ${status.error ? 'has-error' : ''}">
                                <label for="locale" class="col-sm-5 control-label">
                                    <spring:message code="page.config.common.locale"/>
                                </label>
                                <div class="col-sm-7">
                                    <form:select id="locale" path="locale" cssClass="form-control select2" cssStyle="width: 100%;">
                                        <c:forEach items="${locales}" var="locale">
                                            <spring:message code="global.bot.locale.${locale}" var="localeName"/>
                                            <form:option value="${locale}" label="${localeName}" />
                                        </c:forEach>
                                    </form:select>
                                    <form:errors path="locale" class="help-block" />
                                </div>
                            </div>
                        </spring:bind>

                        <spring:bind path="privateHelp">
                            <div class="form-group checkbox-group ${status.error ? 'has-error' : ''}">
                                <label for="input-help" class="col-sm-5 control-label">
                                    <jb:command code="discord.command.help.key" var="helpCommand"/>
                                    <spring:message code="page.config.publish.privateHelp" arguments="${helpCommand}"/>
                                    <i class="fa fa-fw fa-question-circle" data-toggle="tooltip"
                                       title="<spring:message code="page.config.publish.privateHelp.note"/>"
                                       data-container="body"></i>
                                </label>
                                <div class="col-sm-7">
                                    <form:checkbox id="input-help" path="privateHelp"
                                                   data-toggle="toggle"
                                                   data-onstyle="warning"
                                                   data-size="small"
                                                   data-on="${switchOn}"
                                                   data-off="${switchOff}" />
                                    <form:errors path="privateHelp" class="help-block" />
                                </div>
                            </div>
                        </spring:bind>

                        <hr />

                        <spring:bind path="modConfig.roles">
                            <div class="form-group ${status.error ? 'has-error' : ''}">
                                <label for="modRoles" class="col-sm-5 control-label">
                                    <spring:message code="page.config.mod.roles"/>
                                </label>
                                <div class="col-sm-7">
                                    <form:select id="modRoles" path="modConfig.roles" disabled="${not serverAdded}"
                                                 cssClass="form-control select2" cssStyle="width: 100%;"
                                                 items="${roles}" itemValue="idLong" itemLabel="name" multiple="multiple" />
                                    <form:errors path="modConfig.roles" class="help-block" />
                                </div>
                            </div>
                        </spring:bind>
                        <spring:bind path="modConfig.publicColors">
                            <div class="form-group ${status.error ? 'has-error' : ''}">
                                <label for="publicColors" class="col-sm-5 control-label">
                                    <spring:message code="page.config.publicColor.title"/>
                                </label>
                                <div class="col-sm-7">
                                    <form:checkbox id="publicColors" path="modConfig.publicColors"
                                                   data-toggle="toggle"
                                                   data-onstyle="warning"
                                                   data-size="small"
                                                   data-on="${switchOn}"
                                                   data-off="${switchOff}" />
                                    <form:errors path="modConfig.publicColors" class="help-block" />
                                </div>
                            </div>
                        </spring:bind>
                        <spring:bind path="modConfig.maxWarnings">
                            <div class="form-group ${status.error ? 'has-error' : ''}">
                                <label for="maxWarnings" class="col-sm-5 control-label">
                                    <spring:message code="page.config.maxWarnings.title"/>
                                </label>
                                <div class="col-sm-2">
                                    <form:input id="maxWarnings" type="number" min="3" max="20"
                                                path="modConfig.maxWarnings" cssClass="form-control" />
                                    <form:errors path="modConfig.maxWarnings" class="help-block" />
                                </div>
                            </div>
                        </spring:bind>
                    </div>
                    <div class="tab-pane" id="tab_music_config">
                        <spring:bind path="musicConfig.channelId">
                            <div class="form-group ${status.error ? 'has-error' : ''}">
                                <label for="music-channel" class="col-sm-4 control-label">
                                    <spring:message code="page.config.music.channel"/>
                                </label>
                                <div class="col-sm-8">
                                    <form:select id="music-channel" path="musicConfig.channelId" disabled="${not serverAdded}" cssClass="form-control select2" cssStyle="width: 100%;"
                                                 items="${voiceChannels}" itemValue="idLong" itemLabel="name" />
                                    <form:errors path="musicConfig.channelId" class="help-block" />
                                    <spring:bind path="musicConfig.userJoinEnabled">
                                        <div class="checkbox ${status.error ? 'has-error' : ''}" style="padding-top: 0px;">
                                            <label>
                                                <form:checkbox path="musicConfig.userJoinEnabled" cssStyle="margin-top: 4px;" />
                                                <spring:message code="page.config.music.userJoin"/>
                                            </label>
                                            <form:errors path="musicConfig.userJoinEnabled" class="help-block" />
                                        </div>
                                    </spring:bind>
                                </div>
                            </div>
                        </spring:bind>

                        <spring:bind path="musicConfig.textChannelId">
                            <div class="form-group ${status.error ? 'has-error' : ''}">
                                <label for="music-text-channel" class="col-sm-4 control-label">
                                    <spring:message code="page.config.music.textChannel"/>
                                </label>
                                <div class="col-sm-8">
                                    <form:select id="music-text-channel" path="musicConfig.textChannelId" disabled="${not serverAdded}" cssClass="form-control select2" cssStyle="width: 100%;"
                                                 items="${writeableTextChannels}" itemValue="idLong" itemLabel="name" />
                                    <form:errors path="musicConfig.textChannelId" class="help-block" />
                                </div>
                            </div>
                        </spring:bind>

                        <spring:bind path="musicConfig.autoPlay">
                            <div class="form-group ${status.error ? 'has-error' : ''}">
                                <label for="music-autoplay" class="col-sm-4 control-label">
                                    <spring:message code="page.config.music.autoplay"/>
                                    <i class="fa fa-fw fa-question-circle" data-toggle="tooltip"
                                       title="<spring:message code="page.config.music.autoplay.help"/>"
                                       data-container="body"></i>
                                </label>
                                <div class="col-sm-8">
                                    <form:input id="music-autoplay" path="musicConfig.autoPlay" type="text" class="form-control"
                                                placeholder="https://www.youtube.com/watch?v=mghhLqu31cQ" />
                                    <form:errors path="musicConfig.autoPlay" class="help-block" />
                                </div>
                            </div>
                        </spring:bind>

                        <spring:bind path="musicConfig.roles">
                            <div class="form-group ${status.error ? 'has-error' : ''}">
                                <label for="music-roles" class="col-sm-4 control-label">
                                    <spring:message code="page.config.music.roles.title"/>
                                    <i class="fa fa-fw fa-question-circle" data-toggle="tooltip"
                                       title="<spring:message code="page.config.music.roles.help"/>"
                                       data-container="body"></i>
                                </label>
                                <div class="col-sm-8">
                                    <spring:message code="page.config.music.roles.placeholder" var="musicRolesPlaceholder"/>
                                    <form:select id="music-roles" path="musicConfig.roles" disabled="${not serverAdded}"
                                                 cssClass="form-control select2" cssStyle="width: 100%;"
                                                 items="${roles}" itemValue="idLong" itemLabel="name" multiple="multiple"
                                                 data-placeholder="${musicRolesPlaceholder}" />
                                    <form:errors path="musicConfig.roles" class="help-block" />
                                </div>
                            </div>
                        </spring:bind>

                        <div class="form-group">
                            <label class="col-sm-4 control-label combo-control">
                                <spring:message code="page.config.music.limits"/>
                            </label>
                            <div class="col-sm-8">
                                <spring:message code="page.config.music.limits.unlimited" var="unlimitedPlaceholder" />
                                <div class="row">
                                    <spring:bind path="musicConfig.queueLimit">
                                        <div class="col-sm-6 ${status.error ? 'has-error' : ''}">
                                            <label for="queue-limit" class="control-label">
                                                <spring:message code="page.config.music.limits.queue-limit"/>
                                            </label>
                                            <form:input id="queue-limit" type="number" min="1" path="musicConfig.queueLimit" cssClass="form-control"
                                                        placeholder="${unlimitedPlaceholder}" />
                                            <form:errors path="musicConfig.queueLimit" class="help-block" />
                                        </div>
                                    </spring:bind>
                                    <spring:bind path="musicConfig.durationLimit">
                                        <div class="col-sm-6 ${status.error ? 'has-error' : ''}">
                                            <label for="duration-limit" class="control-label">
                                                <spring:message code="page.config.music.limits.duration-limit"/>
                                            </label>
                                            <div class="input-group">
                                                <form:input id="duration-limit" type="number" min="1" path="musicConfig.durationLimit" cssClass="form-control"
                                                            placeholder="${unlimitedPlaceholder}" />
                                                <span class="input-group-addon">
                                                <spring:message code="global.time.minute"/>
                                            </span>
                                            </div>
                                            <form:errors path="musicConfig.durationLimit" class="help-block" />
                                        </div>
                                    </spring:bind>
                                </div>
                                <div class="row">
                                    <spring:bind path="musicConfig.duplicateLimit">
                                        <div class="col-sm-6 ${status.error ? 'has-error' : ''}">
                                            <label for="duplicate-limit" class="control-label">
                                                <spring:message code="page.config.music.limits.duplicate-limit"/>
                                            </label>
                                            <form:input id="duplicate-limit" type="number" min="1" path="musicConfig.duplicateLimit" cssClass="form-control"
                                                        placeholder="${unlimitedPlaceholder}" />
                                            <form:errors path="musicConfig.duplicateLimit" class="help-block" />
                                        </div>
                                    </spring:bind>
                                    <div class="col-sm-6 ${status.error ? 'has-error' : ''}">
                                        <spring:bind path="musicConfig.playlistEnabled">
                                            <div class="checkbox play-checkbox ${status.error ? 'has-error' : ''}">
                                                <label>
                                                    <form:checkbox path="musicConfig.playlistEnabled" cssStyle="margin-top: 4px;" />
                                                    <spring:message code="page.config.music.limits.playlistsEnabled"/>
                                                </label>
                                                <form:errors path="musicConfig.playlistEnabled" class="help-block" />
                                            </div>
                                        </spring:bind>
                                        <spring:bind path="musicConfig.streamsEnabled">
                                            <div class="checkbox ${status.error ? 'has-error' : ''}" style="padding-top: 0px;">
                                                <label>
                                                    <form:checkbox path="musicConfig.streamsEnabled" cssStyle="margin-top: 4px;" />
                                                    <spring:message code="page.config.music.limits.streamsEnabled"/>
                                                </label>
                                                <form:errors path="musicConfig.streamsEnabled" class="help-block" />
                                            </div>
                                        </spring:bind>
                                    </div>
                                </div>
                            </div>
                        </div>
                    </div>
                    <div class="tab-pane" id="tab_publish">
                        <form:hidden path="webHook.available" />
                        <spring:bind path="webHook.channelId">
                            <div class="form-group ${status.error ? 'has-error' : ''}">
                                <label for="publish-channel" class="col-sm-4 control-label">
                                    <spring:message code="page.config.publish.juni"/>
                                </label>
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
                                <div id="vk_edit_${status.index}" class="modal bootstrap-dialog type-warning fade size-normal" role="dialog">
                                    <div class="modal-dialog">
                                        <div class="modal-content">
                                            <div class="modal-header">
                                                <div class="bootstrap-dialog-header">
                                                    <div class="bootstrap-dialog-close-button">
                                                        <button type="button" class="close" data-dismiss="modal">&times;</button>
                                                    </div>
                                                    <div class="bootstrap-dialog-title">
                                                        <spring:message code="page.config.vk.modal.edit.title"/>
                                                    </div>
                                                </div>
                                            </div>
                                            <div class="modal-body">
                                                <div class="bootstrap-dialog-body">
                                                    <div class="bootstrap-dialog-message">
                                                        <p><spring:message code="page.config.vk.modal.edit.content"/></p>
                                                        <ul class="list-group vk-attachments">
                                                            <c:forEach items="${vkAttachmentTypes}" var="attachmentType">
                                                                <li class="list-group-item">
                                                                    <label class="pull-left">
                                                                        <spring:message code="com.vk.api.sdk.objects.wall.WallpostAttachmentType.${attachmentType}"/>
                                                                    </label>
                                                                    <div class="pull-right">
                                                                        <form:checkbox id="vk_cb_${status.index}_${attachmentType}"
                                                                                       path="vkConnections[${status.index}].attachments"
                                                                                       value="${attachmentType}"
                                                                                       data-toggle="toggle"
                                                                                       data-onstyle="warning"
                                                                                       data-size="small"
                                                                                       data-on="${switchOn}"
                                                                                       data-off="${switchOff}"/>
                                                                    </div>
                                                                    <div class="clearfix"></div>
                                                                </li>
                                                            </c:forEach>
                                                        </ul>
                                                    </div>
                                                </div>
                                            </div>
                                            <div class="modal-footer">
                                                <button type="submit" class="btn btn-warning">
                                                    <spring:message code="global.button.save" />
                                                </button>
                                            </div>
                                        </div>
                                    </div>
                                </div>

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
                                                <input id="vk-connection-${status.index}" type="text" class="form-control" disabled
                                                       value="<spring:message code="page.config.vk.awaiting"/>">
                                            </c:if>
                                            <span class="input-group-btn">
                                                <span data-toggle="modal" data-target="#vk_edit_${status.index}">
                                                    <button type="button" class="btn vk-attachments-btn"
                                                            title="<spring:message code="page.config.vk.modal.edit.title"/>"
                                                            data-toggle="tooltip"
                                                            data-container="body"
                                                            data-toggle="modal" data-target="#myModal"
                                                            data-vk-id="${config.vkConnections[status.index].id}"
                                                            data-vk-name="${config.vkConnections[status.index].name}">
                                                        <i class="fa fa-edit"></i>
                                                    </button>
                                                </span>
                                                <button type="button" class="btn btn-danger vk-remove-btn"
                                                        title="<spring:message code="page.config.vk.modal.delete.title"/>"
                                                        data-toggle="tooltip"
                                                        data-container="body"
                                                        data-placement="left"
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
                            <i class="fa fa-vk" style="margin-top: -2px;"></i> <spring:message code="page.config.vk.connectButton"/>
                        </a>

                        <c:if test="${not config.webHook.available}">
                            <div class="callout callout-warning">
                                <p><spring:message code="page.config.webHook.unavailable"/></p>
                            </div>
                        </c:if>
                    </div>
                </div>
            </div>
        </div>
    </div>
    <div class="row">
        <div class="col-md-6">
            <button type="submit" class="btn bg-orange"><spring:message code="global.button.save"/></button>
        </div>
    </div>
</form:form>

<script type="text/javascript" src="<c:url value="/resources/js/vk-connector.js?v=${buildTimestamp}"/>"></script>
<script type="text/javascript">
    $(document).ready(function () {
        var connector = new VkConnector({
            modalDeleteTitle:   '<spring:message code="page.config.vk.modal.delete.title"/>',
            modalDeleteContent: '<spring:message code="page.config.vk.modal.delete.content"/>',
            awaitingConnection: '<spring:message code="page.config.vk.awaiting"/>',
            modalDeleteButton:  '<spring:message code="global.button.delete"/>',
            modalCloseButton:   '<spring:message code="global.button.close"/>',
            somethingIsWrong:   '<spring:message code="global.somethingIsWrong"/>'
        });
        connector.init();

        var $tabs = $("#config-tabs");
        var $tabLinks = $tabs.find("a");

        $tabLinks.click(function(e) {
            e.preventDefault();
            $(this).tab('show');
        });

        $tabLinks.on("shown.bs.tab", function(e) {
            setStored('config.selectedTab', $(e.target).attr("href"));
        });

        var selectedTab = getStored('config.selectedTab');
        if (selectedTab) {
            $tabs.find('a[href="' + selectedTab + '"]').tab('show');
        }

        $tabLinks.each(function() {
            var id = $(this).attr("href").substr(1);
            if ($('#' + id).find('.has-error').length) {
                $(this).find('.error-mark').removeClass('hide');
            }
        })
    });
</script>