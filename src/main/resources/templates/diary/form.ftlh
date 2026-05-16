<#import "/layout/field-errors.ftlh" as errors>
<#import "/spring.ftl" as spring>

<#macro foodEntryForm action submitText>
    <form method="post" action="${action}" class="d-grid gap-3" data-food-entry-form>
        <#include "/layout/csrf.ftlh">

        <div>
            <label class="form-label" for="entryDate">Дата</label>
            <@spring.bind "form.entryDate" />
            <input class="form-control" id="entryDate" type="date" name="entryDate"
                   value="${(form.entryDate.toString())!}" required>
            <@errors.field "form.entryDate" />
        </div>

        <div>
            <label class="form-label" for="mealType">Приём пищи</label>
            <@spring.bind "form.mealType" />
            <select class="form-select" id="mealType" name="mealType" required>
                <option value="">Выберите приём пищи</option>
                <#list mealTypes as mealType>
                    <option value="${mealType.name()}" <#if spring.status.value?? && spring.status.value?string == mealType.name()>selected</#if>>
                        <#switch mealType.name()>
                            <#case "BREAKFAST">Завтрак<#break>
                            <#case "LUNCH">Обед<#break>
                            <#case "DINNER">Ужин<#break>
                            <#case "SNACK">Перекус<#break>
                        </#switch>
                    </option>
                </#list>
            </select>
            <@errors.field "form.mealType" />
        </div>

        <div>
            <label class="form-label" for="productId">Продукт</label>
            <@spring.bind "form.productId" />
            <select class="form-select" id="productId" name="productId" required data-product-id>
                <option value="">Выберите продукт</option>
                <#list products as product>
                    <option value="${product.id}" <#if spring.status.value?? && spring.status.value?string == product.id?string>selected</#if>>
                        ${product.name}
                    </option>
                </#list>
            </select>
            <@errors.field "form.productId" />
        </div>

        <div>
            <label class="form-label" for="grams">Масса, г</label>
            <@spring.bind "form.grams" />
            <input class="form-control" id="grams" type="number" name="grams" value="${spring.status.value!}"
                   min="0.1" max="10000" step="0.1" required data-grams>
            <@errors.field "form.grams" />
        </div>

        <section class="nutrition-preview app-panel" data-nutrition-preview hidden>
            <h2 class="h5 mb-3">Предварительный расчёт</h2>
            <div class="row g-3">
                <div class="col-sm-6 col-lg-3">
                    <div class="preview-item">
                        <span>Калории</span>
                        <strong><span data-preview-calories>0</span> ккал</strong>
                    </div>
                </div>
                <div class="col-sm-6 col-lg-3">
                    <div class="preview-item">
                        <span>Белки</span>
                        <strong><span data-preview-proteins>0</span> г</strong>
                    </div>
                </div>
                <div class="col-sm-6 col-lg-3">
                    <div class="preview-item">
                        <span>Жиры</span>
                        <strong><span data-preview-fats>0</span> г</strong>
                    </div>
                </div>
                <div class="col-sm-6 col-lg-3">
                    <div class="preview-item">
                        <span>Углеводы</span>
                        <strong><span data-preview-carbohydrates>0</span> г</strong>
                    </div>
                </div>
            </div>
        </section>

        <p class="text-danger small mb-0" data-calculation-error hidden></p>

        <div class="d-flex align-items-center gap-2">
            <button class="btn btn-primary" type="submit">${submitText}</button>
            <a class="btn btn-link" href="${springMacroRequestContext.contextPath}/diary?date=${(form.entryDate.toString())!}">Отмена</a>
        </div>
    </form>

    <script src="${springMacroRequestContext.contextPath}/js/diary-entry-form.js"></script>
</#macro>
