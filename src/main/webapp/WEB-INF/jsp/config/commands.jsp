<%@ page contentType="text/html;charset=UTF-8" %>
<%@include file="/WEB-INF/template/include.jsp" %>

<spring:url value="/commands/${serverId}" var="actionUrl"/>

<form:form id="commandsForm" modelAttribute="commandsContainer" method="post" action="${actionUrl}">
    <div class="row">
        <div class="col-md-12">
            <div class="box box-warning">
                <div class="box-header with-border">
                    <h3 class="box-title">Пользовательские команды</h3>
                </div>
                <div id="commandsContainer" class="box-body">
                    <div class="noCommandsItem" style="display: none;">Нет пользовательских команд</div>
                    <c:forEach items="${commandsContainer.commands}" var="command" varStatus="i" begin="0">
                        <div class="row commandRow">
                            <form:hidden path="commands[${i.index}].id" />
                            <div class="col-md-3 form-horizontal">
                                <spring:bind path="commands[${i.index}].key">
                                    <div class="form-group ${status.error ? 'has-error' : ''}">
                                        <label class="col-sm-4 control-label">Команда</label>
                                        <div class="col-sm-8">
                                            <div class="input-group">
                                                <span class="input-group-addon"><c:out value="${commandPrefix}"/></span>
                                                <form:input id="key${i.index}" path="commands[${i.index}].key" cssClass="form-control" placeholder="Название" />
                                            </div>
                                            <form:errors path="commands[${i.index}].key" class="help-block error-message" />
                                        </div>
                                    </div>
                                </spring:bind>
                                <spring:bind path="commands[${i.index}].type">
                                    <div class="form-group ${status.error ? 'has-error' : ''}">
                                        <label class="col-sm-4 control-label">Тип</label>
                                        <div class="col-sm-8">
                                            <form:select path="commands[${i.index}].type" cssClass="form-control" cssStyle="width: 100%;"
                                                         items="${commandTypes}" itemLabel="title" />
                                            <form:errors path="commands[${i.index}].type" class="help-block error-message" />
                                        </div>
                                    </div>
                                </spring:bind>
                            </div>

                            <div class="col-md-9">
                                <spring:bind path="commands[${i.index}].content">
                                    <div class="form-group ${status.error ? 'has-error' : ''}">
                                        <span class="badge badge-delete bg-red removeCommand"><i class="fa fa-remove"></i></span>
                                        <form:textarea path="commands[${i.index}].content" cssClass="form-control" cssStyle="min-height: 83px;"
                                                       rows="3" placeholder="Введите тело команды" />
                                        <form:errors path="commands[${i.index}].content" class="help-block error-message" />
                                    </div>
                                </spring:bind>
                            </div>
                        </div>
                    </c:forEach>

                    <c:if test="${commandsContainer.commands.size() == 0}">
                        <div class="row commandRow defaultRow" style="display: none;">
                            <div class="col-md-3 form-horizontal">
                                <div class="form-group">
                                    <label class="col-sm-4 control-label">Команда</label>
                                    <div class="col-sm-8">
                                        <div class="input-group">
                                            <span class="input-group-addon"><c:out value="${commandPrefix}"/></span>
                                            <input class="form-control" type="text" name="commands[].key" value="" placeholder="Название" />
                                        </div>
                                    </div>
                                </div>
                                <div class="form-group">
                                    <label class="col-sm-4 control-label">Тип</label>
                                    <div class="col-sm-8">
                                        <select name="commands[].type" class="form-control" style="width: 100%;">
                                            <c:forEach items="${commandTypes}" var="type">
                                                <option value="${type}">${type.title}</option>
                                            </c:forEach>
                                        </select>
                                    </div>
                                </div>
                            </div>
                            <div class="col-md-9">
                                <span class="badge badge-delete bg-red removeCommand"><i class="fa fa-remove"></i></span>
                                <textarea name="commands[].content" class="form-control" placeholder="Введите тело команды" rows="3" style="min-height: 83px;"></textarea>
                            </div>
                        </div>
                    </c:if>
                </div>
            </div>
        </div>
    </div>
    <div class="row">
        <div class="col-md-12 submit-group">
            <button type="submit" class="btn bg-orange pull-left">Сохранить изменения</button>
            <a id="addCommand" class="btn btn-success pull-left">Добавить команду</a>
            <a href="?reload" class="btn btn-danger pull-left">Сброс</a>
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