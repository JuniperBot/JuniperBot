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

<spring:url value="/commands/${serverId}" var="actionUrl"/>

<form:form modelAttribute="commandsContainer" method="post" action="${actionUrl}">

    <c:forEach items="${commandTypes}" var="commandType">
        <div class="row">
            <div class="col-md-12">
                <div class="box box-warning">
                    <div class="box-header with-border">
                        <h3 class="box-title"><spring:message code="${commandType.key.toString()}" /></h3>
                        <div class="box-tools pull-right button-xs-group">
                            <a class="btn btn-warning btn-xs check-all">Включить все</a>
                            <a class="btn btn-default btn-xs uncheck-all">Отключить все</a>
                        </div>
                    </div>
                    <div class="box-body no-padding">
                        <table class="table table-striped command-table">
                            <tbody>
                            <c:forEach items="${commandType.value}" var="descriptor">
                                <tr>
                                    <td>
                                        <div class="pull-left">
                                            <h4><span class="label label-warning"><c:out value="${commandPrefix}"/><spring:message code="${descriptor.key}"/></span></h4>
                                            <div><spring:message code="${descriptor.description}"/></div>
                                        </div>
                                        <div class="pull-right command-checkbox">
                                            <form:checkbox path="commands" value="${descriptor.key}" data-toggle="toggle" data-onstyle="warning" data-on="Вкл" data-off="Выкл" />
                                        </div>
                                    </td>
                                </tr>
                            </c:forEach>
                            </tbody>
                        </table>

                    </div>
                </div>
            </div>
        </div>
    </c:forEach>

    <div class="row">
        <div class="col-md-12 submit-group">
            <button type="submit" class="btn bg-orange pull-left">Сохранить изменения</button>
        </div>
    </div>
</form:form>

<script type="text/javascript">
    $(document).ready(function() {
        function check(box, check) {
            $(box).find('.command-checkbox input[type=checkbox]').bootstrapToggle(check ? 'on' : 'off');
        }
        $('.check-all').click(function() { check($(this).closest('.box'), true); });
        $('.uncheck-all').click(function() { check($(this).closest('.box'), false); });
    });
</script>