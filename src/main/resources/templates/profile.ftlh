<#import "/layout/base.ftlh" as layout>
<#import "/layout/field-errors.ftlh" as errors>
<#import "/spring.ftl" as spring>

<@layout.page title="Профиль">
    <section class="form-page">
        <h1>Профиль</h1>

        <#if successMessage??>
            <div class="alert alert-success">${successMessage}</div>
        </#if>

        <#if nutritionNorm??>
            <section class="nutrition-summary">
                <h2>Суточная норма</h2>
                <dl class="nutrition-summary-grid">
                    <div>
                        <dt>Возраст</dt>
                        <dd>${nutritionNorm.age}</dd>
                    </div>
                    <div>
                        <dt>Калории</dt>
                        <dd>${nutritionNorm.calories} ккал</dd>
                    </div>
                    <div>
                        <dt>Белки</dt>
                        <dd>${nutritionNorm.proteins} г</dd>
                    </div>
                    <div>
                        <dt>Жиры</dt>
                        <dd>${nutritionNorm.fats} г</dd>
                    </div>
                    <div>
                        <dt>Углеводы</dt>
                        <dd>${nutritionNorm.carbohydrates} г</dd>
                    </div>
                </dl>
            </section>
        </#if>

        <form method="post" action="/profile" class="profile-form">
            <#include "/layout/csrf.ftlh">

            <div class="form-group">
                <label for="gender">Пол</label>
                <@spring.bind "form.gender" />
                <select id="gender" name="gender" required>
                    <option value="">Выберите пол</option>
                    <#list genders as gender>
                        <option value="${gender.name()}" <#if spring.status.value?? && spring.status.value?string == gender.name()>selected</#if>>
                            <#if gender.name() == "MALE">Мужской<#else>Женский</#if>
                        </option>
                    </#list>
                </select>
                <@errors.field "form.gender" />
            </div>

            <div class="form-group">
                <label for="birthDate">Дата рождения</label>
                <@spring.bind "form.birthDate" />
                <input id="birthDate" type="date" name="birthDate" value="${(form.birthDate.toString())!}" required>
                <@errors.field "form.birthDate" />
            </div>

            <div class="profile-grid">
                <div class="form-group">
                    <label for="heightCm">Рост, см</label>
                    <@spring.bind "form.heightCm" />
                    <input id="heightCm" type="number" name="heightCm" value="${spring.status.value!}" min="1" max="300" step="1" required>
                    <@errors.field "form.heightCm" />
                </div>

                <div class="form-group">
                    <label for="weightKg">Вес, кг</label>
                    <@spring.bind "form.weightKg" />
                    <input id="weightKg" type="number" name="weightKg" value="${spring.status.value!}" min="1" step="0.01" required>
                    <@errors.field "form.weightKg" />
                </div>
            </div>

            <div class="form-group">
                <label for="activityLevel">Активность</label>
                <@spring.bind "form.activityLevel" />
                <select id="activityLevel" name="activityLevel" required>
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

            <div class="form-group">
                <label for="goalId">Цель</label>
                <@spring.bind "form.goalId" />
                <select id="goalId" name="goalId" required>
                    <option value="">Выберите цель</option>
                    <#list goals as goal>
                        <option value="${goal.id}" <#if spring.status.value?? && spring.status.value?string == goal.id?string>selected</#if>>
                            ${goal.name}
                        </option>
                    </#list>
                </select>
                <@errors.field "form.goalId" />
            </div>

            <div class="form-actions">
                <button type="submit">Сохранить профиль</button>
            </div>
        </form>
    </section>
</@layout.page>
