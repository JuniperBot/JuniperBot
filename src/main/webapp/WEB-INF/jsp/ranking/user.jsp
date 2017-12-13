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

<div class="box box-widget widget-server">
    <div class="widget-server-header bg-yellow">
        <h5 class="widget-server-title">Рейтинг участников</h5>
        <h3 class="widget-server-name">${serverName}</h3>
    </div>
    <div class="widget-server-image">
        <img class="img-circle" src="${serverIcon}" alt="Аватар сервера">
    </div>
    <div class="box-footer">
        <div style="margin-top: 20px;">
            <tiles:insertDefinition name="ranking.list">
                <tiles:putAttribute name="members" value="${members}" />
                <tiles:putAttribute name="editable" value="${false}" />
            </tiles:insertDefinition>
        </div>
    </div>
</div>
