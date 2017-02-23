<%@ page language="java" contentType="application/json; charset=UTF-8" pageEncoding="UTF-8"%>
<%@include file="/libs/foundation/global.jsp"%>{
<c:forEach var="entry" items="${properties}" varStatus="loop">
    "<c:out value="${entry.key}"/>": "<c:out value="${entry.value}"/>"<c:if test="${!loop.last}">,</c:if>
</c:forEach>
}