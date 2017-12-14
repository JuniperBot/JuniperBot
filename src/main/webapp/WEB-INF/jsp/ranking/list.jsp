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

<c:if test="${not empty members}">
    <div class="list-group">
        <c:forEach items="${members}" var="member">
            <div class="list-group-item"
                 data-id="${member.member.userId}"
                 data-name="${member.member.effectiveName ? member.member.effectiveName : member.member.name}"
                 data-level="${member.level}">
                <div class="row ${editable ? 'editable-rank' : ''}">
                    <div class="col-lg-4 col-md-5 col-sm-6">
                        <div class="widget-rank">
                            <h3><strong>#${member.rank}</strong></h3>
                        </div>
                        <div class="widget-user-2 widget-member">
                            <div class="widget-user-header">
                                <div class="widget-user-image">
                                    <img class="img-circle" src="${member.member.avatarUrl}" onerror="this.src='/resources/img/noavatar.png'" alt="Аватар">
                                </div>
                                <h3 class="widget-user-username"><c:out value="${member.member.effectiveName}" /></h3>
                                <h5 class="widget-user-desc"><c:out value="${member.member.name}" />#<c:out value="${member.member.discriminator}" /></h5>
                            </div>
                        </div>
                    </div>
                    <div class="col-lg-8 col-md-7 col-sm-6">
                        <div class="progress-group progress-level">
                            <span class="progress-text">Опыт (всего ${member.totalExp})</span>
                            <span class="progress-number"><b>${member.remainingExp}</b>/${member.levelExp}</span>
                            <div class="progress">
                                <div class="progress-bar progress-bar-yellow progress-bar-striped" style="width: ${member.pct}%"></div>
                            </div>
                        </div>
                        <div class="level-circle bg-yellow">
                            <div class="level-value">${member.level}</div>
                            <div class="level-desc">ур.</div>
                        </div>
                        <c:if test="${editable}">
                            <div class="editable-rank-actions">
                                <div class="btn-group">
                                    <a href="#" class="dropdown-toggle"
                                       data-toggle="dropdown"
                                       aria-expanded="false"><i class="fa fa-ellipsis-v toggle-icon"></i></a>
                                    <ul class="dropdown-menu dropdown-menu-right">
                                        <li><a href="#" class="update-level-item"><i class="fa fa-pencil"></i> Изменить уровень</a></li>
                                        <li><a href="#" class="reset-level-item text-red"><i class="fa fa-close"></i> Сбросить</a></li>
                                    </ul>
                                </div>
                            </div>
                        </c:if>
                    </div>
                </div>
            </div>
        </c:forEach>
    </div>
</c:if>

<c:if test="${empty members}">
    <div>Рейтинги отсутствуют</div>
</c:if>
