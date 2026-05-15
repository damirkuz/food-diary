<#import "/layout/base.ftlh" as layout>

<@layout.page title="Ошибка 403">
    <section class="error-page">
        <div class="error-card">
            <span class="eyebrow">Ошибка 403</span>
            <h1 class="h3 mb-2">Доступ закрыт</h1>
            <p class="text-secondary">${message!"У вас нет прав для выполнения этого действия."}</p>

            <#if path??>
                <p class="small text-secondary">Путь: ${path}</p>
            </#if>

            <a class="btn btn-primary" href="/">На главную</a>
        </div>
    </section>
</@layout.page>
