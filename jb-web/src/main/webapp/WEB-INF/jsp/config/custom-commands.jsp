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

<spring:url value="/custom-commands/${serverId}" var="actionUrl"/>

<div id="help-dialog" class="modal fade" role="dialog">
    <div class="modal-dialog">
        <div class="modal-content">
            <div class="modal-header">
                <button type="button" class="close" data-dismiss="modal">&times;</button>
                <h4 class="modal-title"><spring:message code="page.custom.modal.help.title"/></h4>
            </div>
            <div class="modal-body">
                <p><spring:message code="page.custom.modal.help.content"/></p>
                <ol>
                    <li><spring:message code="page.custom.modal.help.content.option1"/></li>
                    <li><spring:message code="page.custom.modal.help.content.option2"/></li>
                </ol>
                <p><spring:message code="page.custom.modal.help.keywords"/></p>
                <ul>
                    <li><code>{author}</code> — <spring:message code="page.custom.modal.help.keywords.author"/></li>
                    <li><code>{guild}</code> — <spring:message code="page.custom.modal.help.keywords.guild"/></li>
                    <li><code>{content}</code> — <spring:message code="page.custom.modal.help.keywords.content"/></li>
                </ul>
            </div>
            <div class="modal-footer">
                <button type="button" class="btn btn-default" data-dismiss="modal">
                    <spring:message code="global.button.close"/>
                </button>
            </div>
        </div>
    </div>
</div>

<form:form id="commandsForm" modelAttribute="commandsContainer" method="post" action="${actionUrl}">
    <div class="row">
        <div class="col-md-12">
            <div class="box box-warning">
                <div class="box-header with-border">
                    <h3 class="box-title"><spring:message code="page.custom.title"/>
                        <i class="fa fa-question-circle" style="cursor: pointer" data-toggle="modal" data-target="#help-dialog"></i></h3>
                </div>
                <div class="box-body no-padding">
                    <table id="commandsContainer" class="table table-striped">
                        <tbody>
                        <tr class="noCommandsItem" style="display: none;">
                            <td><spring:message code="page.custom.no-commands"/></td>
                        </tr>
                        <spring:message code="page.custom.command.name" var="commandNameText"/>
                        <spring:message code="page.custom.command.content" var="commandContentText"/>
                        <c:forEach items="${commandsContainer.commands}" var="command" varStatus="i" begin="0">
                            <tr class="commandRow">
                                <td>
                                    <form:hidden path="commands[${i.index}].id" />
                                    <div class="col-md-3 form-horizontal">
                                        <spring:bind path="commands[${i.index}].key">
                                            <div class="form-group ${status.error ? 'has-error' : ''}">
                                                <label class="col-sm-4 control-label">
                                                    <spring:message code="page.custom.command"/>
                                                </label>
                                                <div class="col-sm-8">
                                                    <div class="input-group">
                                                        <span class="input-group-addon"><c:out value="${commandPrefix}"/></span>
                                                        <form:input id="key${i.index}" path="commands[${i.index}].key"
                                                                    cssClass="form-control"
                                                                    placeholder="${commandNameText}" />
                                                    </div>
                                                    <form:errors path="commands[${i.index}].key" class="help-block error-message" />
                                                </div>
                                            </div>
                                        </spring:bind>
                                        <spring:bind path="commands[${i.index}].type">
                                            <div class="form-group no-b-margin ${status.error ? 'has-error' : ''}">
                                                <label class="col-sm-4 control-label">
                                                    <spring:message code="page.custom.command.type"/>
                                                </label>
                                                <div class="col-sm-8">
                                                    <form:select path="commands[${i.index}].type" cssClass="form-control" cssStyle="width: 100%;">
                                                        <c:forEach items="${commandTypes}" var="type">
                                                            <spring:message code="${type.toString()}" var="optionLabel" />
                                                            <form:option value="${type}" label="${optionLabel}" />
                                                        </c:forEach>
                                                    </form:select>
                                                    <form:errors path="commands[${i.index}].type" class="help-block error-message" />
                                                </div>
                                            </div>
                                        </spring:bind>
                                    </div>

                                    <div class="col-md-9">
                                        <spring:bind path="commands[${i.index}].content">
                                            <div class="form-group no-b-margin ${status.error ? 'has-error' : ''}">
                                                <span class="badge badge-delete bg-red removeCommand"><i class="fa fa-remove"></i></span>
                                                <form:textarea path="commands[${i.index}].content" cssClass="form-control" cssStyle="min-height: 83px;"
                                                               rows="3" placeholder="${commandContentText}" />
                                                <form:errors path="commands[${i.index}].content" class="help-block error-message" />
                                            </div>
                                        </spring:bind>
                                    </div>
                                </td>
                            </tr>
                        </c:forEach>
                        <c:if test="${commandsContainer.commands.size() == 0}">
                            <tr class="commandRow defaultRow" style="display: none;">
                                <td>
                                    <div class="col-md-3 form-horizontal">
                                        <div class="form-group">
                                            <label class="col-sm-4 control-label">
                                                <spring:message code="page.custom.command"/>
                                            </label>
                                            <div class="col-sm-8">
                                                <div class="input-group">
                                                    <span class="input-group-addon"><c:out value="${commandPrefix}"/></span>
                                                    <input class="form-control" type="text" name="commands[].key"
                                                           value="" placeholder="${commandNameText}" />
                                                </div>
                                            </div>
                                        </div>
                                        <div class="form-group no-b-margin">
                                            <label class="col-sm-4 control-label">
                                                <spring:message code="page.custom.command.type"/>
                                            </label>
                                            <div class="col-sm-8">
                                                <select name="commands[].type" class="form-control" style="width: 100%;">
                                                    <c:forEach items="${commandTypes}" var="type">
                                                        <option value="${type}"><spring:message code="${type.toString()}" /></option>
                                                    </c:forEach>
                                                </select>
                                            </div>
                                        </div>
                                    </div>
                                    <div class="col-md-9">
                                        <div class="form-group no-b-margin">
                                            <span class="badge badge-delete bg-red removeCommand"><i class="fa fa-remove"></i></span>
                                            <textarea name="commands[].content" class="form-control" placeholder="${commandContentText}" rows="3" style="min-height: 83px;"></textarea>
                                        </div>
                                    </div>
                                </td>
                            </tr>
                        </c:if>
                        </tbody>
                    </table>
                </div>
            </div>
        </div>
    </div>
    <div class="row">
        <div class="col-md-12 submit-group">
            <button type="submit" class="btn bg-orange pull-left"><spring:message code="global.button.save"/></button>
            <a id="addCommand" class="btn btn-success pull-left"><spring:message code="page.custom.button.add"/></a>
            <a href="?reload" class="btn btn-danger pull-left"><spring:message code="global.button.reset"/></a>
        </div>
    </div>
</form:form>

<script type="text/javascript">
    function rowAdded(rowElement) {
        $(rowElement).find("input, textarea").val('');
        $(rowElement).find("select").prop("selectedIndex", 0);
        $(rowElement).find(".error-message").remove();
        $(rowElement).find(".form-group").removeClass('has-error');
        $(rowElement).show();
        saveNeeded();
    }

    function rowRemoved(rowElement) {
        saveNeeded();
    }

    function saveNeeded() {
        // mark some flags
    }

    function beforeSubmit() {
        return true;
    }

    $(document).ready(function () {
        new DynamicListHelper({
            rowClass: 'commandRow',
            addRowId: 'addCommand',
            removeRowClass: 'removeCommand',
            emptyClass: 'noCommandsItem',
            formId: 'commandsForm',
            rowContainerId: 'commandsContainer',
            indexedPropertyName: 'commands',
            indexedPropertyMemberNames: 'id,key,type,content',
            rowAddedListener: rowAdded,
            rowRemovedListener: rowRemoved,
            beforeSubmit: beforeSubmit
        });
    });
</script>