<header class="site-header">
    <nav>
        <a href="/">Главная</a>

        <#if Session?? && Session.SPRING_SECURITY_CONTEXT??>
            <a href="/profile">Профиль</a>

            <form method="post" action="/logout" class="logout-form">
                <#include "/layout/csrf.ftlh">
                <button type="submit" class="link-button">Выйти</button>
            </form>
        <#else>
            <a href="/login">Вход</a>
            <a href="/register">Регистрация</a>
        </#if>
    </nav>
</header>
