<#import "/layout/base.ftlh" as layout>

<@layout.page title="Продукты">
    <section class="page-header">
        <div>
            <h1>Продукты</h1>
            <p>Список продуктов для дневника питания.</p>
        </div>

        <a class="button-link" href="/products/new">Добавить продукт</a>
    </section>

    <#if successMessage??>
        <div class="alert alert-success">${successMessage}</div>
    </#if>

    <#if errorMessage??>
        <div class="alert alert-error">${errorMessage}</div>
    </#if>

    <form method="get" action="/products" class="filter-form">
        <div class="form-group">
            <label for="search">Поиск</label>
            <input id="search" name="search" value="${search}" maxlength="255">
        </div>

        <div class="form-group">
            <label for="sort">Сортировка</label>
            <select id="sort" name="sort">
                <option value="createdAt,desc" <#if sort == "createdAt,desc">selected</#if>>Сначала новые</option>
                <option value="createdAt,asc" <#if sort == "createdAt,asc">selected</#if>>Сначала старые</option>
                <option value="name,asc" <#if sort == "name,asc">selected</#if>>Название А-Я</option>
                <option value="name,desc" <#if sort == "name,desc">selected</#if>>Название Я-А</option>
            </select>
        </div>

        <button type="submit">Показать</button>
    </form>

    <#if products.content?size == 0>
        <p class="empty-state">Пока нет продуктов.</p>
    <#else>
        <div class="table-wrap">
            <table class="data-table">
                <thead>
                <tr>
                    <th>Название</th>
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
                        <td>${product.name}</td>
                        <td>${product.caloriesPer100g}</td>
                        <td>${product.proteinsPer100g}</td>
                        <td>${product.fatsPer100g}</td>
                        <td>${product.carbohydratesPer100g}</td>
                        <td class="table-actions">
                            <a href="/products/${product.id}/edit">Изменить</a>

                            <form method="post" action="/products/${product.id}/delete">
                                <#include "/layout/csrf.ftlh">
                                <button type="submit" class="danger-button">Удалить</button>
                            </form>
                        </td>
                    </tr>
                </#list>
                </tbody>
            </table>
        </div>

        <nav class="pagination">
            <#if products.hasPrevious()>
                <a href="/products?page=${products.number - 1}&size=${products.size}&sort=${sort}&search=${search?url}">Назад</a>
            </#if>

            <span>${products.number + 1} / ${products.totalPages}</span>

            <#if products.hasNext()>
                <a href="/products?page=${products.number + 1}&size=${products.size}&sort=${sort}&search=${search?url}">Вперёд</a>
            </#if>
        </nav>
    </#if>
</@layout.page>
