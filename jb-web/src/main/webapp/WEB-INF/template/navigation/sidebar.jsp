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

<aside class="main-sidebar">
    <section class="sidebar">
        <ul class="sidebar-menu" data-widget="tree">
            <c:forEach items="${navigationMenu}" var="item">
                <tiles:insertDefinition name="menu.item">
                    <tiles:putAttribute name="item" value="${item}" />
                </tiles:insertDefinition>
            </c:forEach>
        </ul>
    </section>
</aside>
<script type="text/javascript">
    $(document).ready(function () {
        $('.sidebar-menu').tree();
        $('.sidebar-toggle').click(function(event) {
            event.preventDefault();
            setStored('sidebar-toggle-collapsed', Boolean(getStored('sidebar-toggle-collapsed')) ? '' : 1 );
        });
    });
</script>