<#import "/layout/base.ftlh" as layout>
<#import "/layout/alerts.ftlh" as alerts>

<@layout.page title="Продукты">
    <section class="page-header mb-4">
        <div>
            <span class="eyebrow">Справочник</span>
            <h1 class="h2 mb-1">Продукты</h1>
            <p class="text-secondary mb-0">Список продуктов для дневника питания.</p>
        </div>

        <a class="btn btn-primary" href="/products/new">Добавить продукт</a>
    </section>

    <@alerts.flash />

    <section class="app-panel mb-4">
        <form method="get" action="/products" class="row g-3 align-items-end">
            <div class="col-lg-6">
                <label class="form-label" for="search">Поиск</label>
                <input class="form-control" id="search" name="search" value="${search}" maxlength="255"
                       placeholder="Например, овсянка">
            </div>

            <div class="col-lg-4">
                <label class="form-label" for="sort">Сортировка</label>
                <select class="form-select" id="sort" name="sort">
                    <option value="createdAt,desc" <#if sort == "createdAt,desc">selected</#if>>Сначала новые</option>
                    <option value="createdAt,asc" <#if sort == "createdAt,asc">selected</#if>>Сначала старые</option>
                    <option value="name,asc" <#if sort == "name,asc">selected</#if>>Название А-Я</option>
                    <option value="name,desc" <#if sort == "name,desc">selected</#if>>Название Я-А</option>
                </select>
            </div>

            <div class="col-lg-2 d-grid">
                <button class="btn btn-outline-primary" type="submit">Показать</button>
            </div>
        </form>
    </section>

    <#if products.content?size == 0>
        <div class="empty-state">
            <h2 class="h5">Пока нет продуктов</h2>
            <p class="text-secondary mb-3">Добавьте первый продукт вручную или подтяните БЖУ через внешний API.</p>
            <a class="btn btn-primary" href="/products/new">Добавить продукт</a>
        </div>
    <#else>
        <div class="table-responsive app-table">
            <table class="table align-middle mb-0">
                <thead>
                <tr>
                    <th>Название</th>
                    <th>Ккал / 100 г</th>
                    <th>Белки</th>
                    <th>Жиры</th>
                    <th>Углеводы</th>
                    <th></th>
                </tr>
                </thead>
                <tbody>
                <#list products.content as product>
                    <tr>
                        <td>
                            <a class="fw-semibold text-decoration-none" href="/products/${product.id}">${product.name}</a>
                            <#if product.public>
                                <span class="badge text-bg-info ms-2">общее</span>
                            </#if>
                        </td>
                        <td>${product.caloriesPer100g}</td>
                        <td>${product.proteinsPer100g}</td>
                        <td>${product.fatsPer100g}</td>
                        <td>${product.carbohydratesPer100g}</td>
                        <td>
                            <div class="d-flex justify-content-end gap-2">
                                <#if !product.public>
                                    <a class="btn btn-outline-primary btn-sm" href="/products/${product.id}/edit">Изменить</a>
                                </#if>

                                <#if !product.public>
                                    <form method="post" action="/products/${product.id}/delete" class="m-0">
                                        <#include "/layout/csrf.ftlh">
                                        <button type="submit" class="btn btn-outline-danger btn-sm">Удалить</button>
                                    </form>
                                </#if>
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
                   href="/products?page=${products.number - 1}&size=${products.size}&sort=${sort}&search=${search?url}">Назад</a>
            </#if>

            <span class="text-secondary small">${products.number + 1} / ${products.totalPages}</span>

            <#if products.hasNext()>
                <a class="btn btn-outline-primary btn-sm"
                   href="/products?page=${products.number + 1}&size=${products.size}&sort=${sort}&search=${search?url}">Вперёд</a>
            </#if>
        </nav>
    </#if>
</@layout.page>
