<#import "/layout/field-errors.ftlh" as errors>
<#import "/spring.ftl" as spring>

<#macro productForm action submitText>
    <form method="post" action="${action}" class="product-form" data-product-form>
        <#include "/layout/csrf.ftlh">

        <div class="form-group">
            <label for="name">Название</label>
            <@spring.bind "form.name" />
            <input id="name" name="name" value="${spring.status.value!}" required maxlength="255" data-product-name>
            <@errors.field "form.name" />
        </div>

        <div class="lookup-actions">
            <button type="button" class="secondary-button" data-lookup-product>Подтянуть БЖУ по названию</button>
            <span class="lookup-status" data-lookup-status></span>
        </div>

        <div class="nutrition-grid">
            <div class="form-group">
                <label for="caloriesPer100g">Ккал на 100 г</label>
                <@spring.bind "form.caloriesPer100g" />
                <input id="caloriesPer100g" type="number" name="caloriesPer100g" value="${spring.status.value!}" min="0" step="0.01" required data-calories>
                <@errors.field "form.caloriesPer100g" />
            </div>

            <div class="form-group">
                <label for="proteinsPer100g">Белки на 100 г</label>
                <@spring.bind "form.proteinsPer100g" />
                <input id="proteinsPer100g" type="number" name="proteinsPer100g" value="${spring.status.value!}" min="0" step="0.01" required data-proteins>
                <@errors.field "form.proteinsPer100g" />
            </div>

            <div class="form-group">
                <label for="fatsPer100g">Жиры на 100 г</label>
                <@spring.bind "form.fatsPer100g" />
                <input id="fatsPer100g" type="number" name="fatsPer100g" value="${spring.status.value!}" min="0" step="0.01" required data-fats>
                <@errors.field "form.fatsPer100g" />
            </div>

            <div class="form-group">
                <label for="carbohydratesPer100g">Углеводы на 100 г</label>
                <@spring.bind "form.carbohydratesPer100g" />
                <input id="carbohydratesPer100g" type="number" name="carbohydratesPer100g" value="${spring.status.value!}" min="0" step="0.01" required data-carbohydrates>
                <@errors.field "form.carbohydratesPer100g" />
            </div>
        </div>

        <div class="form-actions">
            <button type="submit">${submitText}</button>
            <a href="/products">Отмена</a>
        </div>
    </form>

    <script src="/js/product-lookup-form.js"></script>
</#macro>
