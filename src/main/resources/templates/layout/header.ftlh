<#assign headerAuthenticated = isAuthenticated!false>
<#assign headerAdmin = isAdmin!false>
<#assign headerEmail = currentUserEmail!>

<#if !headerAuthenticated && Session?? && Session.SPRING_SECURITY_CONTEXT??>
    <#assign sessionAuth = Session.SPRING_SECURITY_CONTEXT.authentication>
    <#if sessionAuth?? && sessionAuth.name != "anonymousUser">
        <#assign headerAuthenticated = true>
        <#assign headerEmail = sessionAuth.name>

        <#list sessionAuth.authorities as authority>
            <#if authority.authority == "ROLE_ADMIN">
                <#assign headerAdmin = true>
            </#if>
        </#list>
    </#if>
</#if>

<header class="site-header sticky-top">
    <nav class="navbar navbar-expand-lg bg-white border-bottom">
        <div class="container">
            <a class="navbar-brand fw-bold text-primary" href="${springMacroRequestContext.contextPath}/">
                Food Diary
            </a>

            <button class="navbar-toggler" type="button" data-bs-toggle="collapse" data-bs-target="#mainNav"
                    aria-controls="mainNav" aria-expanded="false" aria-label="Открыть меню">
                <span class="navbar-toggler-icon"></span>
            </button>

            <div class="collapse navbar-collapse" id="mainNav">
                <ul class="navbar-nav me-auto mb-2 mb-lg-0">
                    <li class="nav-item">
                        <a class="nav-link" href="${springMacroRequestContext.contextPath}/">Главная</a>
                    </li>

                    <#if headerAuthenticated>
                        <li class="nav-item">
                            <a class="nav-link" href="${springMacroRequestContext.contextPath}/diary">Дневник</a>
                        </li>
                        <li class="nav-item">
                            <a class="nav-link" href="${springMacroRequestContext.contextPath}/products">Продукты</a>
                        </li>
                        <li class="nav-item">
                            <a class="nav-link" href="${springMacroRequestContext.contextPath}/profile">Профиль</a>
                        </li>

                        <#if headerAdmin>
                            <li class="nav-item dropdown">
                                <a class="nav-link dropdown-toggle" href="#" role="button" data-bs-toggle="dropdown"
                                   aria-expanded="false">
                                    Админка
                                </a>
                                <ul class="dropdown-menu">
                                    <li><a class="dropdown-item" href="${springMacroRequestContext.contextPath}/admin/users">Пользователи</a></li>
                                    <li><a class="dropdown-item" href="${springMacroRequestContext.contextPath}/admin/products">Все продукты</a></li>
                                </ul>
                            </li>
                        </#if>
                    </#if>
                </ul>

                <div class="d-flex align-items-lg-center gap-3 flex-column flex-lg-row">
                    <#if headerAuthenticated>
                        <span class="small text-secondary">${headerEmail}</span>
                        <form method="post" action="${springMacroRequestContext.contextPath}/logout" class="m-0">
                            <#include "/layout/csrf.ftlh">
                            <button type="submit" class="btn btn-outline-primary btn-sm">Выйти</button>
                        </form>
                    <#else>
                        <a class="btn btn-outline-primary btn-sm" href="${springMacroRequestContext.contextPath}/login">Вход</a>
                        <a class="btn btn-primary btn-sm" href="${springMacroRequestContext.contextPath}/register">Регистрация</a>
                    </#if>
                </div>
            </div>
        </div>
    </nav>
</header>
