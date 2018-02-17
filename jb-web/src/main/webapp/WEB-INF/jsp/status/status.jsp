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

<div class="row common-stats">
    <div class="col-md-3 col-sm-6 col-xs-12">
        <div class="info-box">
            <span class="info-box-icon bg-yellow"><i class="fa fa-users"></i></span>
            <div class="info-box-content">
                <span class="info-box-text"><spring:message code="page.status.common.servers"/></span>
                <span class="info-box-number"><spring:message code="global.singleFormat" arguments="${guildCount}"/></span>
            </div>
        </div>
    </div>
    <div class="col-md-3 col-sm-6 col-xs-12">
        <div class="info-box">
            <span class="info-box-icon bg-orange"><i class="fa fa-user"></i></span>
            <div class="info-box-content">
                <span class="info-box-text"><spring:message code="page.status.common.users"/></span>
                <span class="info-box-number"><spring:message code="global.singleFormat" arguments="${userCount}"/></span>
            </div>
        </div>
    </div>
    <div class="col-md-3 col-sm-6 col-xs-12">
        <div class="info-box">
            <span class="info-box-icon bg-yellow sharp-icon">#</span>
            <div class="info-box-content">
                <span class="info-box-text">
                    <spring:message code="page.status.common.channels"/>
                </span>
                <span class="info-box-more" style="margin-top: 5px;">
                    <spring:message code="page.status.common.channels.text"/>:
                    <span class="pull-right badge bg-yellow"><spring:message code="global.singleFormat" arguments="${textChannelCount}"/></span>
                </span>
                <span class="info-box-more">
                    <spring:message code="page.status.common.channels.voice"/>:
                    <span class="pull-right badge bg-yellow"><spring:message code="global.singleFormat" arguments="${voiceChannelCount}"/></span>
                </span>
            </div>
        </div>
    </div>
    <div class="col-md-3 col-sm-6 col-xs-12">
        <div class="info-box">
            <span class="info-box-icon bg-orange"><i class="fa fa-music"></i></span>
            <div class="info-box-content">
                <span class="info-box-text"><spring:message code="page.status.common.voiceConnections"/></span>
                <span class="info-box-number"><spring:message code="global.singleFormat" arguments="${activeConnections}"/></span>
            </div>
        </div>
    </div>
</div>

<div class="row">
    <div class="col-md-4">
        <div class="box box-warning">
            <div class="box-header with-border">
                <i class="fa fa-database"></i>
                <h3 class="box-title"><spring:message code="page.statistics.title"/></h3>
            </div>
            <div class="box-footer no-padding">
                <ul class="nav nav-stacked">
                    <li>
                        <a href="#">
                            <span><spring:message code="page.statistics.uptime"/></span>
                            <span class="text-bold">${jvmUptime}</span>
                        </a>
                    </li>
                    <li>
                        <a href="#">
                            <span><spring:message code="page.statistics.commands.executions.persist"/></span>
                            <span class="pull-right badge bg-yellow">
                                <spring:message code="global.singleFormat" arguments="${executedCommands}"/>
                            </span>
                        </a>
                    </li>
                    <li>
                        <a href="#">
                            <span><spring:message code="page.statistics.commands.executions.rate.mean"/></span>
                            <span class="pull-right badge bg-orange">
                                <spring:message code="global.singleFormat" arguments="${commandsRateMean}"/>
                            </span>
                        </a>
                    </li>
                    <li>
                        <a href="#">
                            <span><spring:message code="page.statistics.commands.executions.rate.1m"/></span>
                            <span class="pull-right badge bg-yellow">
                                <spring:message code="global.singleFormat" arguments="${commandsRate1m}"/>
                            </span>
                        </a>
                    </li>
                    <li>
                        <a href="#">
                            <span><spring:message code="page.statistics.commands.executions.rate.5m"/></span>
                            <span class="pull-right badge bg-orange">
                                <spring:message code="global.singleFormat" arguments="${commandsRate5m}"/>
                            </span>
                        </a>
                    </li>
                    <li>
                        <a href="#">
                            <span><spring:message code="page.statistics.commands.executions.rate.15m"/></span>
                            <span class="pull-right badge bg-yellow">
                                <spring:message code="global.singleFormat" arguments="${commandsRate15m}"/>
                            </span>
                        </a>
                    </li>
                </ul>
            </div>
        </div>
    </div>
    <div class="col-md-8">
        <div class="box box-warning">
            <div class="box-header with-border">
                <i class="fa fa-bar-chart-o"></i>
                <h3 class="box-title"><spring:message code="page.status.ping.title"/></h3>
            </div>
            <div class="box-body">
                <div id="ping-chart" style="height: 225px;"></div>
            </div>
        </div>
    </div>
</div>

<script type="text/javascript" src="<c:url value="/resources/webjars/flot/0.8.3/jquery.flot.min.js"/>"></script>
<script type="text/javascript" src="<c:url value="/resources/webjars/flot/0.8.3/jquery.flot.resize.min.js"/>"></script>
<script type="text/javascript" src="<c:url value="/resources/webjars/flot/0.8.3/jquery.flot.time.min.js"/>"></script>
<script type="text/javascript" src="<c:url value="/resources/webjars/flot/0.8.3/jquery.flot.navigate.min.js"/>"></script>
<script type="text/javascript" src="<c:url value="/resources/webjars/flot-axislabels/1.0.0/jquery.flot.axislabels.js"/>"></script>
<script type="text/javascript" src="<c:url value="/resources/js/status.js?v=${buildTimestamp}"/>"></script>
<script type="text/javascript">
    $(document).ready(function () {
        var status = new Status({
            timeLabel: '<spring:message code="page.status.ping.timeLabel"/>',
            pingLabel: '<spring:message code="page.status.ping.pingLabel"/>',
            pingPostfix: '<spring:message code="page.status.ping.pingLabel.postfix"/>',
            disconnected: '<spring:message code="page.status.ping.disconnected"/>'
        });
        status.init();
    });
</script>