
<%@ page contentType="text/html;charset=UTF-8" %>
<%@include file="/WEB-INF/template/include.jsp" %>

<form action="<c:url value="/login"/>" method="post">
    <div><label> User Name : <input type="text" name="username"/> </label></div>
    <div><label> Password: <input type="password" name="password"/> </label></div>
    <div><input type="submit" value="Sign In"/></div>
</form>
