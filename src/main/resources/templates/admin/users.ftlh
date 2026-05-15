<#import "/layout/base.ftlh" as layout>
<#import "/layout/alerts.ftlh" as alerts>

<#function hasAdminRole user>
    <#list user.roleNames as roleName>
        <#if roleName == "ROLE_ADMIN">
            <#return true>
        </#if>
    </#list>
    <#return false>
</#function>

<@layout.page title="Админка · Пользователи">
    <section class="page-header mb-4">
        <div>
            <span class="eyebrow">Администрирование</span>
            <h1 class="h2 mb-1">Пользователи</h1>
            <p class="text-secondary mb-0">Управление доступом, блокировкой и ролью администратора.</p>
        </div>

        <a class="btn btn-outline-primary" href="${springMacroRequestContext.contextPath}/admin/products">Все продукты</a>
    </section>

    <@alerts.flash />

    <section class="app-panel mb-4">
        <form method="get" action="${springMacroRequestContext.contextPath}/admin/users" class="row g-3 align-items-end">
            <div class="col-lg-10">
                <label class="form-label" for="search">Поиск по email</label>
                <input class="form-control" id="search" name="search" value="${search}" maxlength="254">
            </div>
            <div class="col-lg-2 d-grid">
                <button class="btn btn-outline-primary" type="submit">Показать</button>
            </div>
        </form>
    </section>

    <div class="table-responsive app-table">
        <table class="table align-middle mb-0">
            <thead>
            <tr>
                <th>Email</th>
                <th>Роли</th>
                <th>Статус</th>
                <th>Создан</th>
                <th></th>
            </tr>
            </thead>
            <tbody>
            <#list users.content as user>
                <#assign adminRole = hasAdminRole(user)>
                <#assign isSelf = user.id == currentUserId>
                <tr>
                    <td class="fw-semibold">${user.email}</td>
                    <td>
                        <#list user.roleNames as roleName>
                            <span class="badge text-bg-primary-subtle text-primary">${roleName}</span>
                        </#list>
                    </td>
                    <td>
                        <#if user.enabled>
                            <span class="badge text-bg-success">Активен</span>
                        <#else>
                            <span class="badge text-bg-danger">Заблокирован</span>
                        </#if>
                    </td>
                    <td class="text-secondary">${user.createdAt}</td>
                    <td>
                        <div class="dropdown text-end">
                            <button class="btn btn-outline-primary btn-sm dropdown-toggle" type="button"
                                    data-bs-toggle="dropdown" aria-expanded="false">
                                Действия
                            </button>
                            <div class="dropdown-menu dropdown-menu-end action-menu">
                                <form method="post" action="${springMacroRequestContext.contextPath}/admin/users/${user.id}/role" class="m-0">
                                    <#include "/layout/csrf.ftlh">
                                    <input type="hidden" name="admin" value="${(!adminRole)?string('true', 'false')}">
                                    <button class="dropdown-item" type="submit" <#if isSelf && adminRole>disabled</#if>>
                                        <#if adminRole>Снять админку<#else>Выдать админку</#if>
                                    </button>
                                </form>

                                <form method="post" action="${springMacroRequestContext.contextPath}/admin/users/${user.id}/status" class="m-0">
                                    <#include "/layout/csrf.ftlh">
                                    <input type="hidden" name="enabled" value="${(!user.enabled)?string('true', 'false')}">
                                    <button class="dropdown-item text-danger" type="submit" <#if isSelf && user.enabled>disabled</#if>>
                                        <#if user.enabled>Заблокировать<#else>Разблокировать</#if>
                                    </button>
                                </form>
                            </div>
                        </div>
                    </td>
                </tr>
            </#list>
            </tbody>
        </table>
    </div>

    <nav class="pagination-nav">
        <#if users.hasPrevious()>
            <a class="btn btn-outline-primary btn-sm"
               href="${springMacroRequestContext.contextPath}/admin/users?page=${users.number - 1}&size=${users.size}&search=${search?url}">Назад</a>
        </#if>
        <span class="text-secondary small">${users.number + 1} / ${users.totalPages}</span>
        <#if users.hasNext()>
            <a class="btn btn-outline-primary btn-sm"
               href="${springMacroRequestContext.contextPath}/admin/users?page=${users.number + 1}&size=${users.size}&search=${search?url}">Вперёд</a>
        </#if>
    </nav>
</@layout.page>
