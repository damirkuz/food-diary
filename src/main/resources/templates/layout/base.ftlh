<#macro page title>
    <!DOCTYPE html>
    <html lang="ru">
    <head>
        <meta charset="UTF-8">
        <meta name="viewport" content="width=device-width, initial-scale=1">
        <meta name="_context_path" content="${springMacroRequestContext.contextPath}">
        <title>${title} | Food Diary</title>

        <#if _csrf??>
            <meta name="_csrf" content="${_csrf.token}">
            <meta name="_csrf_header" content="${_csrf.headerName}">
        </#if>

        <link rel="stylesheet" href="${springMacroRequestContext.contextPath}/webjars/bootstrap/5.3.3/css/bootstrap.min.css">
        <link rel="stylesheet" href="${springMacroRequestContext.contextPath}/css/main.css">
    </head>
    <body class="app-body">
    <#include "/layout/header.ftlh">

    <main class="container app-main">
        <#nested>
    </main>

    <#include "/layout/footer.ftlh">
    <script src="${springMacroRequestContext.contextPath}/webjars/bootstrap/5.3.3/js/bootstrap.bundle.min.js"></script>
    </body>
    </html>
</#macro>
