<#import "/layout/base.ftlh" as layout>

<@layout.page title="Ошибка">
    <section class="auth-page">
        <h1>Ошибка</h1>

        <p>${message!"Произошла внутренняя ошибка сервера"}</p>

        <#if path??>
            <p>Путь: ${path}</p>
        </#if>

        <p>
            <a href="/login">Вернуться ко входу</a>
        </p>
    </section>
</@layout.page>
