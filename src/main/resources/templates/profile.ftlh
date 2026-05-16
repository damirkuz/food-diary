<#import "/layout/base.ftlh" as layout>
<#import "/layout/alerts.ftlh" as alerts>
<#import "/layout/field-errors.ftlh" as errors>
<#import "/spring.ftl" as spring>

<@layout.page title="Профиль">
    <section class="page-header mb-4">
        <div>
            <span class="eyebrow">Личный кабинет</span>
            <h1 class="h2 mb-1">Профиль</h1>
            <p class="text-secondary mb-0">Параметры для расчёта суточной нормы калорий и БЖУ.</p>
        </div>
    </section>

    <@alerts.flash />

    <#if nutritionNorm??>
        <section class="row g-4 mb-4">
            <div class="col-md-4 col-xl">
                <div class="metric-card">
                    <span>Возраст</span>
                    <strong>${nutritionNorm.age}</strong>
                    <small>лет</small>
                </div>
            </div>
            <div class="col-md-4 col-xl">
                <div class="metric-card">
                    <span>Калории</span>
                    <strong>${nutritionNorm.calories}</strong>
                    <small>ккал</small>
                </div>
            </div>
            <div class="col-md-4 col-xl">
                <div class="metric-card">
                    <span>Белки</span>
                    <strong>${nutritionNorm.proteins}</strong>
                    <small>г</small>
                </div>
            </div>
            <div class="col-md-4 col-xl">
                <div class="metric-card">
                    <span>Жиры</span>
                    <strong>${nutritionNorm.fats}</strong>
                    <small>г</small>
                </div>
            </div>
            <div class="col-md-4 col-xl">
                <div class="metric-card">
                    <span>Углеводы</span>
                    <strong>${nutritionNorm.carbohydrates}</strong>
                    <small>г</small>
                </div>
            </div>
        </section>
    </#if>

    <section class="form-shell">
        <div class="app-panel">
            <h2 class="h4 mb-3">Данные профиля</h2>

            <form method="post" action="${springMacroRequestContext.contextPath}/profile" class="d-grid gap-3">
                <#include "/layout/csrf.ftlh">

                <div>
                    <label class="form-label" for="gender">Пол</label>
                    <@spring.bind "form.gender" />
                    <select class="form-select" id="gender" name="gender" required>
                        <option value="">Выберите пол</option>
                        <#list genders as gender>
                            <option value="${gender.name()}" <#if spring.status.value?? && spring.status.value?string == gender.name()>selected</#if>>
                                <#if gender.name() == "MALE">Мужской<#else>Женский</#if>
                            </option>
                        </#list>
                    </select>
                    <@errors.field "form.gender" />
                </div>

                <div>
                    <label class="form-label" for="birthDate">Дата рождения</label>
                    <@spring.bind "form.birthDate" />
                    <input class="form-control" id="birthDate" type="date" name="birthDate"
                           value="${(form.birthDate.toString())!}" min="${birthDateMin}" max="${birthDateMax}" required>
                    <@errors.field "form.birthDate" />
                </div>

                <div class="row g-3">
                    <div class="col-md-6">
                        <label class="form-label" for="heightCm">Рост, см</label>
                        <@spring.bind "form.heightCm" />
                        <input class="form-control" id="heightCm" type="number" name="heightCm"
                               value="${spring.status.value!}" min="50" max="250" step="1" required>
                        <@errors.field "form.heightCm" />
                    </div>

                    <div class="col-md-6">
                        <label class="form-label" for="weightKg">Вес, кг</label>
                        <@spring.bind "form.weightKg" />
                        <input class="form-control" id="weightKg" type="number" name="weightKg"
                               value="${spring.status.value!}" min="2" max="500" step="0.01" required>
                        <@errors.field "form.weightKg" />
                    </div>
                </div>

                <div>
                    <label class="form-label" for="activityLevel">Активность</label>
                    <@spring.bind "form.activityLevel" />
                    <select class="form-select" id="activityLevel" name="activityLevel" required>
                        <option value="">Выберите уровень активности</option>
                        <#list activityLevels as activityLevel>
                            <option value="${activityLevel.name()}" <#if spring.status.value?? && spring.status.value?string == activityLevel.name()>selected</#if>>
                                <#switch activityLevel.name()>
                                    <#case "SEDENTARY">Минимальная<#break>
                                    <#case "LIGHT">Лёгкая<#break>
                                    <#case "MODERATE">Умеренная<#break>
                                    <#case "HIGH">Высокая<#break>
                                    <#case "VERY_HIGH">Очень высокая<#break>
                                </#switch>
                            </option>
                        </#list>
                    </select>
                    <@errors.field "form.activityLevel" />
                </div>

                <div>
                    <label class="form-label" for="goalId">Цель</label>
                    <@spring.bind "form.goalId" />
                    <select class="form-select" id="goalId" name="goalId" required>
                        <option value="">Выберите цель</option>
                        <#list goals as goal>
                            <option value="${goal.id}" <#if spring.status.value?? && spring.status.value?string == goal.id?string>selected</#if>>
                                ${goal.name}
                            </option>
                        </#list>
                    </select>
                    <@errors.field "form.goalId" />
                </div>

                <div>
                    <button class="btn btn-primary" type="submit">Сохранить профиль</button>
                </div>
            </form>
        </div>
    </section>
</@layout.page>
