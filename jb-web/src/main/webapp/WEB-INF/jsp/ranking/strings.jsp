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

<script type="text/javascript">
    var rankingLocale = {
        noRoles:                '<spring:message code="page.ranking.rewards.noRoles"/>',
        sortAsc:                '<spring:message code="global.dataTables.sort.asc"/>',
        sortDesc:               '<spring:message code="global.dataTables.sort.desc"/>',
        somethingIsWrong:       '<spring:message code="global.somethingIsWrong"/>',
        updateModalTitle:       '<spring:message code="page.ranking.update.modal.title"/>',
        closeButtonText:        '<spring:message code="global.button.close"/>',
        resetAllButtonText:     '<spring:message code="global.button.resetAll"/>',
        resetAllModalTitle:     '<spring:message code="page.ranking.resetAll.modal.title"/>',
        resetAllModalContent:   "<spring:message code="page.ranking.resetAll.modal.content"/>", // dirty hack to not escape ' in string
        importButtonText:       '<spring:message code="page.ranking.admin.modal.import.button"/>',
        importModalTitle:       '<spring:message code="page.ranking.admin.modal.import.title"/>',
        resetButtonText:        '<spring:message code="global.button.reset"/>',
        resetModalTitle:        '<spring:message code="page.ranking.reset.modal.title"/>',
        resetModalContent:      '<spring:message code="page.ranking.reset.modal.content"/>'
    };
</script>
