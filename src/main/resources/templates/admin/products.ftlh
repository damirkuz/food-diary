<#import "/layout/base.ftlh" as layout>
<#import "/layout/alerts.ftlh" as alerts>

<@layout.page title="Админка · Продукты">
    <section class="page-header mb-4">
        <div>
            <span class="eyebrow">Администрирование</span>
            <h1 class="h2 mb-1">Все продукты</h1>
            <p class="text-secondary mb-0">Просмотр и управление продуктами всех пользователей.</p>
        </div>

        <a class="btn btn-outline-primary" href="/admin/users">Пользователи</a>
    </section>

    <@alerts.flash />

    <section class="app-panel mb-4">
        <form method="get" action="/admin/products" class="row g-3 align-items-end">
            <div class="col-lg-6">
                <label class="form-label" for="search">Поиск</label>
                <input class="form-control" id="search" name="search" value="${search}" maxlength="255">
            </div>
            <div class="col-lg-4">
                <label class="form-label" for="sort">Сортировка</label>
                <select class="form-select" id="sort" name="sort">
                    <option value="createdAt,desc" <#if sort == "createdAt,desc">selected</#if>>Сначала новые</option>
                    <option value="createdAt,asc" <#if sort == "createdAt,asc">selected</#if>>Сначала старые</option>
                    <option value="name,asc" <#if sort == "name,asc">selected</#if>>Название А-Я</option>
                    <option value="name,desc" <#if sort == "name,desc">selected</#if>>Название Я-А</option>
                    <option value="owner,asc" <#if sort == "owner,asc">selected</#if>>Владелец А-Я</option>
                    <option value="owner,desc" <#if sort == "owner,desc">selected</#if>>Владелец Я-А</option>
                </select>
            </div>
            <div class="col-lg-2 d-grid">
                <button class="btn btn-outline-primary" type="submit">Показать</button>
            </div>
        </form>
    </section>

    <#if products.content?size == 0>
        <div class="empty-state">Продуктов пока нет.</div>
    <#else>
        <div class="table-responsive app-table">
            <table class="table align-middle mb-0">
                <thead>
                <tr>
                    <th>Название</th>
                    <th>Владелец</th>
                    <th>Ккал</th>
                    <th>Белки</th>
                    <th>Жиры</th>
                    <th>Углеводы</th>
                    <th></th>
                </tr>
                </thead>
                <tbody>
                <#list products.content as product>
                    <tr>
                        <td class="fw-semibold">${product.name}</td>
                        <td>${product.owner.email}</td>
                        <td>${product.caloriesPer100g?string["0.#"]}</td>
                        <td>${product.proteinsPer100g?string["0.#"]}</td>
                        <td>${product.fatsPer100g?string["0.#"]}</td>
                        <td>${product.carbohydratesPer100g?string["0.#"]}</td>
                        <td>
                            <div class="d-flex justify-content-end gap-2">
                                <a class="btn btn-outline-primary btn-sm" href="/admin/products/${product.id}/edit">Изменить</a>

                                <form method="post" action="/admin/products/${product.id}/delete" class="m-0">
                                    <#include "/layout/csrf.ftlh">
                                    <button class="btn btn-outline-danger btn-sm" type="submit">Удалить</button>
                                </form>
                            </div>
                        </td>
                    </tr>
                </#list>
                </tbody>
            </table>
        </div>

        <nav class="pagination-nav">
            <#if products.hasPrevious()>
                <a class="btn btn-outline-primary btn-sm"
                   href="/admin/products?page=${products.number - 1}&size=${products.size}&sort=${sort}&search=${search?url}">Назад</a>
            </#if>
            <span class="text-secondary small">${products.number + 1} / ${products.totalPages}</span>
            <#if products.hasNext()>
                <a class="btn btn-outline-primary btn-sm"
                   href="/admin/products?page=${products.number + 1}&size=${products.size}&sort=${sort}&search=${search?url}">Вперёд</a>
            </#if>
        </nav>
    </#if>
</@layout.page>
