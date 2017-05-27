<%@ taglib tagdir="/WEB-INF/tags" prefix="t" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<c:set var="jsIncludes">
    <script type="text/javascript" src="/js/checkBrowserVersion.js"></script>
    <script type="text/javascript" src="/js/index.js"></script>
</c:set>
<t:staticPage jsIncludes="${jsIncludes}" currentPage="index">
        <h1 class="h2 color_orange text-center">
           Fail! You can <a href="/canvas">re-authorize</a>.
        </h1>
</t:staticPage>
