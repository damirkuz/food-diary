<#macro page title>
    <!DOCTYPE html>
    <html lang="ru">
    <head>
        <meta charset="UTF-8">
        <title>${title}</title>

        <#if _csrf??>
            <meta name="_csrf" content="${_csrf.token}">
            <meta name="_csrf_header" content="${_csrf.headerName}">
        </#if>

        <link rel="stylesheet" href="/css/main.css">
    </head>
    <body>
    <#include "/layout/header.ftlh">

    <main class="container">
        <#nested>
    </main>

    <#include "/layout/footer.ftlh">
    </body>
    </html>
</#macro>