<#import "/layout/base.ftlh" as layout>

<@layout.page title="Ошибка 500">
    <section class="error-page">
        <div class="error-card">
            <span class="eyebrow">Ошибка 500</span>
            <h1 class="h3 mb-2">Что-то пошло не так</h1>
            <p class="text-secondary">${message!"Произошла внутренняя ошибка сервера"}</p>

            <#if path??>
                <p class="small text-secondary">Путь: ${path}</p>
            </#if>

            <a class="btn btn-primary" href="${springMacroRequestContext.contextPath}/">На главную</a>
        </div>
    </section>
</@layout.page>
