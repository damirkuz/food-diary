<#import "/layout/base.ftlh" as layout>
<#import "/layout/alerts.ftlh" as alerts>

<#function mealTypeLabel value>
    <#switch value.name()>
        <#case "BREAKFAST"><#return "Завтрак">
        <#case "LUNCH"><#return "Обед">
        <#case "DINNER"><#return "Ужин">
        <#case "SNACK"><#return "Перекус">
        <#default><#return value.name()>
    </#switch>
</#function>

<#function percent actual target>
    <#if !target?? || target == 0>
        <#return 0>
    </#if>
    <#return ((actual / target) * 100)?round>
</#function>

<@layout.page title="Дневник">
    <section class="page-header mb-4">
        <div>
            <span class="eyebrow">Дневник</span>
            <h1 class="h2 mb-1">Дневник питания</h1>
            <p class="text-secondary mb-0">Записи и итоги за выбранный день.</p>
        </div>

        <a class="btn btn-primary" href="/diary/entries/new?date=${selectedDate}">Добавить запись</a>
    </section>

    <@alerts.flash />

    <section class="app-panel mb-4">
        <form method="get" action="/diary" class="row g-3 align-items-end">
            <div class="col-sm-8 col-lg-4">
                <label class="form-label" for="date">Дата</label>
                <input class="form-control" id="date" type="date" name="date" value="${selectedDate}" required>
            </div>

            <div class="col-sm-4 col-lg-2 d-grid">
                <button class="btn btn-outline-primary" type="submit">Показать</button>
            </div>
        </form>
    </section>

    <section class="row g-4 mb-4">
        <div class="col-md-3">
            <div class="metric-card">
                <span>Калории</span>
                <strong>${totals.calories?string["0.#"]}</strong>
                <small>ккал</small>
            </div>
        </div>
        <div class="col-md-3">
            <div class="metric-card">
                <span>Белки</span>
                <strong>${totals.proteins?string["0.#"]}</strong>
                <small>г</small>
            </div>
        </div>
        <div class="col-md-3">
            <div class="metric-card">
                <span>Жиры</span>
                <strong>${totals.fats?string["0.#"]}</strong>
                <small>г</small>
            </div>
        </div>
        <div class="col-md-3">
            <div class="metric-card">
                <span>Углеводы</span>
                <strong>${totals.carbohydrates?string["0.#"]}</strong>
                <small>г</small>
            </div>
        </div>
    </section>

    <#if dailyNorm??>
        <section class="app-panel mb-4">
            <div class="d-flex flex-wrap justify-content-between gap-2 mb-3">
                <div>
                    <h2 class="h5 mb-1">Сравнение с нормой</h2>
                    <p class="text-secondary mb-0">Возраст: ${dailyNorm.age}</p>
                </div>
                <a class="btn btn-outline-primary btn-sm" href="/profile">Изменить профиль</a>
            </div>

            <div class="row g-3">
                <div class="col-md-3">
                    <div class="comparison-item">
                        <span>Калории</span>
                        <strong>${totals.calories?string["0.#"]} / ${dailyNorm.calories?string["0.#"]} ккал</strong>
                        <small>${percent(totals.calories, dailyNorm.calories)}%</small>
                    </div>
                </div>
                <div class="col-md-3">
                    <div class="comparison-item">
                        <span>Белки</span>
                        <strong>${totals.proteins?string["0.#"]} / ${dailyNorm.proteins?string["0.#"]} г</strong>
                        <small>${percent(totals.proteins, dailyNorm.proteins)}%</small>
                    </div>
                </div>
                <div class="col-md-3">
                    <div class="comparison-item">
                        <span>Жиры</span>
                        <strong>${totals.fats?string["0.#"]} / ${dailyNorm.fats?string["0.#"]} г</strong>
                        <small>${percent(totals.fats, dailyNorm.fats)}%</small>
                    </div>
                </div>
                <div class="col-md-3">
                    <div class="comparison-item">
                        <span>Углеводы</span>
                        <strong>${totals.carbohydrates?string["0.#"]} / ${dailyNorm.carbohydrates?string["0.#"]} г</strong>
                        <small>${percent(totals.carbohydrates, dailyNorm.carbohydrates)}%</small>
                    </div>
                </div>
            </div>
        </section>
    <#else>
        <div class="empty-state mb-4">
            <h2 class="h5">Нет расчёта нормы</h2>
            <p class="text-secondary mb-3">Заполните профиль, чтобы видеть сравнение с дневной нормой.</p>
            <a class="btn btn-primary" href="/profile">Заполнить профиль</a>
        </div>
    </#if>

    <#if entries?size == 0>
        <div class="empty-state">
            <h2 class="h5">За этот день записей пока нет</h2>
            <p class="text-secondary mb-3">Добавьте приём пищи, чтобы увидеть итоги и сравнение с нормой.</p>
            <a class="btn btn-primary" href="/diary/entries/new?date=${selectedDate}">Добавить запись</a>
        </div>
    <#else>
        <div class="table-responsive app-table">
            <table class="table align-middle mb-0">
                <thead>
                <tr>
                    <th>Приём пищи</th>
                    <th>Продукт</th>
                    <th>Граммы</th>
                    <th>Ккал</th>
                    <th>Белки</th>
                    <th>Жиры</th>
                    <th>Углеводы</th>
                    <th></th>
                </tr>
                </thead>
                <tbody>
                <#list entries as entry>
                    <tr>
                        <td>${mealTypeLabel(entry.mealType)}</td>
                        <td>${entry.product.name}</td>
                        <td>${entry.grams?string["0.#"]}</td>
                        <td>${entry.calories?string["0.#"]}</td>
                        <td>${entry.proteins?string["0.#"]}</td>
                        <td>${entry.fats?string["0.#"]}</td>
                        <td>${entry.carbohydrates?string["0.#"]}</td>
                        <td>
                            <div class="d-flex justify-content-end gap-2">
                                <a class="btn btn-outline-primary btn-sm" href="/diary/entries/${entry.id}/edit">Изменить</a>

                                <form method="post" action="/diary/entries/${entry.id}/delete" class="m-0">
                                    <#include "/layout/csrf.ftlh">
                                    <input type="hidden" name="date" value="${selectedDate}">
                                    <button type="submit" class="btn btn-outline-danger btn-sm">Удалить</button>
                                </form>
                            </div>
                        </td>
                    </tr>
                </#list>
                </tbody>
            </table>
        </div>
    </#if>

    <#if highCalorieEntries?size gt 0>
        <section class="app-panel mt-4">
            <h2 class="h5 mb-3">Наиболее калорийные записи дня</h2>
            <ul class="list-group list-group-flush">
                <#list highCalorieEntries as entry>
                    <li class="list-group-item px-0 d-flex justify-content-between">
                        <span>${entry.product.name}</span>
                        <strong>${entry.calories?string["0.#"]} ккал</strong>
                    </li>
                </#list>
            </ul>
        </section>
    </#if>
</@layout.page>
