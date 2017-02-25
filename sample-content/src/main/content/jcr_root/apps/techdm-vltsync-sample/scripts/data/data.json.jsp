<%--
  
  Copyright 2017 Daniel Henrique Alves Lima
  
  Licensed under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License.
  You may obtain a copy of the License at
  
      http://www.apache.org/licenses/LICENSE-2.0
   
  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.
  
--%><%@ page language="java" contentType="application/json; charset=UTF-8" pageEncoding="UTF-8"--%>
<%@include file="/libs/foundation/global.jsp"%>{
<c:forEach var="entry" items="${properties}" varStatus="loop">
    "<c:out value="${entry.key}"/>": "<c:out value="${entry.value}"/>"<c:if test="${!loop.last}">,</c:if>
</c:forEach>
}