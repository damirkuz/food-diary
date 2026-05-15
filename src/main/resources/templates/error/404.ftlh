<#import "/layout/base.ftlh" as layout>

<@layout.page title="Ошибка 404">
    <section class="error-page">
        <div class="error-card">
            <span class="eyebrow">Ошибка 404</span>
            <h1 class="h3 mb-2">Страница не найдена</h1>
            <p class="text-secondary">${message!"Такой страницы или ресурса нет."}</p>

            <#if path??>
                <p class="small text-secondary">Путь: ${path}</p>
            </#if>

            <a class="btn btn-primary" href="${springMacroRequestContext.contextPath}/">На главную</a>
        </div>
    </section>
</@layout.page>
