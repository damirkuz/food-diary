<#import "/layout/base.ftlh" as layout>

<@layout.page title="Ошибка 400">
    <section class="error-page">
        <div class="error-card">
            <span class="eyebrow">Ошибка 400</span>
            <h1 class="h3 mb-2">Некорректный запрос</h1>
            <p class="text-secondary">${message!"Проверьте данные и повторите действие."}</p>

            <#if path??>
                <p class="small text-secondary">Путь: ${path}</p>
            </#if>

            <a class="btn btn-primary" href="${springMacroRequestContext.contextPath}/">На главную</a>
        </div>
    </section>
</@layout.page>
