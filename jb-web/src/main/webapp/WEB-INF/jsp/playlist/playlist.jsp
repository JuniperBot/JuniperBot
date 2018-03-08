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

<div class="box box-widget widget-server <c:if test="${not empty serverIcon}">bg-image</c:if>">
    <div class="widget-server-header bg-yellow">
        <div class="widget-server-background" style="background-image: url('/api/blur?source=<c:url value="${serverIcon}"/>');">
            <div class="widget-bottom-panel"></div>
        </div>
        <div class="row widget-server-header-row">
            <div class="col-md-7">
                <div class="pull-left widget-header-name">
                    <h5 class="widget-server-title"><spring:message code="page.playlist.title"/></h5>
                    <h3 class="widget-server-name">${serverName}</h3>
                </div>
            </div>
            <div class="col-md-5">
                <div class="callout pull-right">
                    <h4 class="pull-left">
                        <spring:message code="page.playlist.howto"/>
                    </h4>
                    <div class="clearfix"></div>
                    <jb:command code="discord.command.play.key" var="playCommand"/>
                    <p><spring:message code="page.playlist.howto.content" arguments="${prefix};${playCommand}" argumentSeparator=";"/></p>
                </div>
            </div>
        </div>
    </div>

    <div class="widget-server-image">
        <img class="img-circle" src="${serverIcon}" alt="<spring:message code="global.title.serverAvatar"/>">
    </div>
    <div class="box-footer">
        <div class="widget-server-container">
            <c:if test="${not empty playlist.items}">
                <c:url value="/resources/img/noavatar.png" var="noThumbnailUrl"/>
                <div class="list-group list-group-stripped list-group-hover">
                    <c:forEach items="${playlist.items}" var="item">
                        <div class="list-group-item">
                            <div class="row">
                                <div class="col-md-12">
                                    <div class="widget-rank">
                                        <h3><strong>#${item.index + 1}</strong></h3>
                                    </div>
                                    <div class="widget-user-2 widget-member widget-compact">
                                        <div class="widget-user-header">
                                            <div class="widget-user-image">
                                                <div class="widget-img" style="background-image: url('${not empty item.artworkUri ? item.artworkUri : noThumbnailUrl}');"></div>
                                            </div>
                                            <h3 class="widget-user-username">
                                                <span>
                                                    <c:if test="${not empty item.uri}">
                                                        <a href="${item.uri}" target="_blank">
                                                            <c:out value="${item.title}" />
                                                        </a>
                                                    </c:if>
                                                    <c:if test="${empty item.uri}">
                                                        <c:out value="${item.title}" />
                                                    </c:if>
                                                </span>
                                                <c:if test="${not item.stream and item.length > 0}">
                                                    <span class="label label-warning">
                                                        <c:out value="${jb:formatDuration(item.length)}"/>
                                                    </span>
                                                </c:if>
                                                <c:if test="${item.stream}">
                                                    <span class="text-red">
                                                        <i class="fa fa-fw fa-feed"></i>
                                                    </span>
                                                </c:if>
                                            </h3>
                                            <h5 class="widget-user-desc"><c:out value="${item.author}" /></h5>
                                        </div>
                                    </div>
                                </div>

                            </div>
                        </div>
                    </c:forEach>
                </div>
            </c:if>
        </div>
    </div>
</div>
