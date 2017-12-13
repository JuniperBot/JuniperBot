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

<tilesx:useAttribute id="members" name="members" classname="java.util.List" />
<tilesx:useAttribute id="editable" name="editable" classname="java.lang.Boolean" />

<c:forEach items="${item.childs}" var="child">
    <tiles:insertDefinition name="menu.item">
        <tiles:putAttribute name="item" value="${child}" />
    </tiles:insertDefinition>
</c:forEach>

<c:if test="${not empty members}">
    <div class="list-group">
        <c:forEach items="${members}" var="member">
        <div class="list-group-item">
            <div class="row">
                <div class="col-md-5">
                    <div class="pull-left" style="margin-top: 7px;">
                        <h3><strong>#${member.rank}</strong></h3>
                    </div>
                    <div class="pull-left">
                        <div class="widget-user-2 widget-user-2-sm">
                            <div class="widget-user-header">
                                <div class="widget-user-image">
                                    <img class="img-circle" src="${member.member.avatarUrl}" onerror="this.src='/resources/img/noavatar.png'" alt="Аватар">
                                </div>
                                <h3 class="widget-user-username"><c:out value="${member.member.effectiveName}" /></h3>
                                <h5 class="widget-user-desc"><c:out value="${member.member.name}" />#<c:out value="${member.member.discriminator}" /></h5>
                            </div>
                        </div>
                    </div>
                </div>
                <div class="col-md-7">
                    <div class="pull-right">
                        <div class="level-circle bg-yellow">
                            <div class="level-value">${member.level}</div>
                            <div class="level-desc">ур.</div>
                        </div>
                    </div>
                    <div class="col-md-9 col-sm-12 col-xs-12 pull-right">
                        <div class="progress-group progress-level">
                            <span class="progress-text">Опыт (всего ${member.totalExp})</span>
                            <span class="progress-number"><b>${member.remainingExp}</b>/${member.levelExp}</span>
                            <div class="progress">
                                <div class="progress-bar progress-bar-yellow progress-bar-striped" style="width: ${member.pct}%"></div>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
            </c:forEach>
        </div>
    </div>
</c:if>

<c:if test="${empty members}">
    <div>Рейтинги отсутствуют</div>
</c:if>
