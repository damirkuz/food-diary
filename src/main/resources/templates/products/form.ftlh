<#import "/layout/field-errors.ftlh" as errors>
<#import "/spring.ftl" as spring>

<#macro productForm action submitText cancelUrl="/products">
    <form method="post" action="${action}" class="d-grid gap-3" data-product-form>
        <#include "/layout/csrf.ftlh">

        <div>
            <label class="form-label" for="name">Название</label>
            <@spring.bind "form.name" />
            <input class="form-control" id="name" name="name" value="${spring.status.value!}" required maxlength="255"
                   data-product-name>
            <@errors.field "form.name" />
        </div>

        <div class="lookup-actions">
            <button type="button" class="btn btn-outline-primary" data-lookup-product>
                Подтянуть БЖУ по названию
            </button>
            <span class="lookup-status small" data-lookup-status></span>
        </div>

        <div class="row g-3">
            <div class="col-md-6">
                <label class="form-label" for="caloriesPer100g">Ккал на 100 г</label>
                <@spring.bind "form.caloriesPer100g" />
                <input class="form-control" id="caloriesPer100g" type="number" name="caloriesPer100g"
                       value="${spring.status.value!}" min="0" max="1000" step="0.01" required data-calories>
                <@errors.field "form.caloriesPer100g" />
            </div>

            <div class="col-md-6">
                <label class="form-label" for="proteinsPer100g">Белки на 100 г</label>
                <@spring.bind "form.proteinsPer100g" />
                <input class="form-control" id="proteinsPer100g" type="number" name="proteinsPer100g"
                       value="${spring.status.value!}" min="0" max="100" step="0.01" required data-proteins>
                <@errors.field "form.proteinsPer100g" />
            </div>

            <div class="col-md-6">
                <label class="form-label" for="fatsPer100g">Жиры на 100 г</label>
                <@spring.bind "form.fatsPer100g" />
                <input class="form-control" id="fatsPer100g" type="number" name="fatsPer100g"
                       value="${spring.status.value!}" min="0" max="100" step="0.01" required data-fats>
                <@errors.field "form.fatsPer100g" />
            </div>

            <div class="col-md-6">
                <label class="form-label" for="carbohydratesPer100g">Углеводы на 100 г</label>
                <@spring.bind "form.carbohydratesPer100g" />
                <input class="form-control" id="carbohydratesPer100g" type="number" name="carbohydratesPer100g"
                       value="${spring.status.value!}" min="0" max="100" step="0.01" required data-carbohydrates>
                <@errors.field "form.carbohydratesPer100g" />
            </div>
        </div>

        <div class="d-flex align-items-center gap-2">
            <button class="btn btn-primary" type="submit">${submitText}</button>
            <a class="btn btn-link" href="${springMacroRequestContext.contextPath}${cancelUrl}">Отмена</a>
        </div>
    </form>

    <script src="${springMacroRequestContext.contextPath}/js/product-lookup-form.js"></script>
</#macro>
