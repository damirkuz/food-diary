<#import "/layout/base.ftlh" as layout>

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
    <section class="page-header">
        <div>
            <h1>Дневник питания</h1>
            <p>Записи и итоги за выбранный день.</p>
        </div>

        <a class="button-link" href="/diary/entries/new?date=${selectedDate}">Добавить запись</a>
    </section>

    <#if successMessage??>
        <div class="alert alert-success">${successMessage}</div>
    </#if>

    <form method="get" action="/diary" class="filter-form diary-date-form">
        <div class="form-group">
            <label for="date">Дата</label>
            <input id="date" type="date" name="date" value="${selectedDate}" required>
        </div>

        <button type="submit">Показать</button>
    </form>

    <section class="diary-summary">
        <div class="summary-card">
            <h2>Итого</h2>
            <dl class="nutrition-summary-grid">
                <div>
                    <dt>Калории</dt>
                    <dd>${totals.calories?string["0.#"]} ккал</dd>
                </div>
                <div>
                    <dt>Белки</dt>
                    <dd>${totals.proteins?string["0.#"]} г</dd>
                </div>
                <div>
                    <dt>Жиры</dt>
                    <dd>${totals.fats?string["0.#"]} г</dd>
                </div>
                <div>
                    <dt>Углеводы</dt>
                    <dd>${totals.carbohydrates?string["0.#"]} г</dd>
                </div>
            </dl>
        </div>

        <#if dailyNorm??>
            <div class="summary-card">
                <h2>Сравнение с нормой</h2>
                <div class="comparison-grid">
                    <div>
                        <span>Калории</span>
                        <strong>${totals.calories?string["0.#"]} / ${dailyNorm.calories?string["0.#"]} ккал</strong>
                        <small>${percent(totals.calories, dailyNorm.calories)}%</small>
                    </div>
                    <div>
                        <span>Белки</span>
                        <strong>${totals.proteins?string["0.#"]} / ${dailyNorm.proteins?string["0.#"]} г</strong>
                        <small>${percent(totals.proteins, dailyNorm.proteins)}%</small>
                    </div>
                    <div>
                        <span>Жиры</span>
                        <strong>${totals.fats?string["0.#"]} / ${dailyNorm.fats?string["0.#"]} г</strong>
                        <small>${percent(totals.fats, dailyNorm.fats)}%</small>
                    </div>
                    <div>
                        <span>Углеводы</span>
                        <strong>${totals.carbohydrates?string["0.#"]} / ${dailyNorm.carbohydrates?string["0.#"]} г</strong>
                        <small>${percent(totals.carbohydrates, dailyNorm.carbohydrates)}%</small>
                    </div>
                </div>
            </div>
        <#else>
            <p class="empty-state">Заполните профиль, чтобы видеть сравнение с дневной нормой.</p>
        </#if>
    </section>

    <#if entries?size == 0>
        <p class="empty-state">За этот день записей пока нет.</p>
    <#else>
        <div class="table-wrap">
            <table class="data-table">
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
                        <td class="table-actions">
                            <a href="/diary/entries/${entry.id}/edit">Изменить</a>

                            <form method="post" action="/diary/entries/${entry.id}/delete">
                                <#include "/layout/csrf.ftlh">
                                <input type="hidden" name="date" value="${selectedDate}">
                                <button type="submit" class="danger-button">Удалить</button>
                            </form>
                        </td>
                    </tr>
                </#list>
                </tbody>
            </table>
        </div>
    </#if>

    <#if highCalorieEntries?size gt 0>
        <section class="high-calorie">
            <h2>Наиболее калорийные записи дня</h2>
            <ul>
                <#list highCalorieEntries as entry>
                    <li>${entry.product.name}: ${entry.calories?string["0.#"]} ккал</li>
                </#list>
            </ul>
        </section>
    </#if>
</@layout.page>
