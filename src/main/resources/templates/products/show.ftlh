<#import "/layout/base.ftlh" as layout>

<@layout.page title="${product.name}">
    <section class="page-header mb-4">
        <div>
            <span class="eyebrow">Карточка продукта</span>
            <h1 class="h2 mb-1">
                ${product.name}
                <#if product.public>
                    <span class="badge text-bg-info align-middle">общее</span>
                </#if>
            </h1>
            <p class="text-secondary mb-0">Пищевая ценность указана на 100 граммов.</p>
        </div>

        <div class="d-flex gap-2">
            <a class="btn btn-outline-primary" href="${springMacroRequestContext.contextPath}/products">Назад</a>
            <#if !product.public>
                <a class="btn btn-primary" href="${springMacroRequestContext.contextPath}/products/${product.id}/edit">Изменить</a>
            </#if>
        </div>
    </section>

    <section class="row g-4">
        <div class="col-md-3">
            <div class="metric-card">
                <span>Калории</span>
                <strong>${product.caloriesPer100g?string["0.#"]}</strong>
                <small>ккал</small>
            </div>
        </div>
        <div class="col-md-3">
            <div class="metric-card">
                <span>Белки</span>
                <strong>${product.proteinsPer100g?string["0.#"]}</strong>
                <small>г</small>
            </div>
        </div>
        <div class="col-md-3">
            <div class="metric-card">
                <span>Жиры</span>
                <strong>${product.fatsPer100g?string["0.#"]}</strong>
                <small>г</small>
            </div>
        </div>
        <div class="col-md-3">
            <div class="metric-card">
                <span>Углеводы</span>
                <strong>${product.carbohydratesPer100g?string["0.#"]}</strong>
                <small>г</small>
            </div>
        </div>
    </section>

    <section class="app-panel mt-4">
        <dl class="row mb-0">
            <dt class="col-sm-3 text-secondary">Создан</dt>
            <dd class="col-sm-9">${product.createdAt}</dd>
            <dt class="col-sm-3 text-secondary">Обновлён</dt>
            <dd class="col-sm-9 mb-0">${product.updatedAt}</dd>
        </dl>
    </section>
</@layout.page>
