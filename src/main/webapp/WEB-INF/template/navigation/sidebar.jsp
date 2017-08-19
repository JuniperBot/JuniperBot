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