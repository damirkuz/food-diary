<#import "/layout/field-errors.ftlh" as errors>
<#import "/spring.ftl" as spring>

<#macro foodEntryForm action submitText>
    <form method="post" action="${action}" class="food-entry-form" data-food-entry-form>
        <#include "/layout/csrf.ftlh">

        <div class="form-group">
            <label for="entryDate">Дата</label>
            <@spring.bind "form.entryDate" />
            <input id="entryDate" type="date" name="entryDate" value="${(form.entryDate.toString())!}" required>
            <@errors.field "form.entryDate" />
        </div>

        <div class="form-group">
            <label for="mealType">Приём пищи</label>
            <@spring.bind "form.mealType" />
            <select id="mealType" name="mealType" required>
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

        <div class="form-group">
            <label for="productId">Продукт</label>
            <@spring.bind "form.productId" />
            <select id="productId" name="productId" required data-product-id>
                <option value="">Выберите продукт</option>
                <#list products as product>
                    <option value="${product.id}" <#if spring.status.value?? && spring.status.value?string == product.id?string>selected</#if>>
                        ${product.name}
                    </option>
                </#list>
            </select>
            <@errors.field "form.productId" />
        </div>

        <div class="form-group">
            <label for="grams">Масса, г</label>
            <@spring.bind "form.grams" />
            <input id="grams" type="number" name="grams" value="${spring.status.value!}" min="0.1" step="0.1" required data-grams>
            <@errors.field "form.grams" />
        </div>

        <section class="nutrition-preview" data-nutrition-preview hidden>
            <h2>Предварительный расчёт</h2>
            <dl class="nutrition-summary-grid">
                <div>
                    <dt>Калории</dt>
                    <dd><span data-preview-calories>0</span> ккал</dd>
                </div>
                <div>
                    <dt>Белки</dt>
                    <dd><span data-preview-proteins>0</span> г</dd>
                </div>
                <div>
                    <dt>Жиры</dt>
                    <dd><span data-preview-fats>0</span> г</dd>
                </div>
                <div>
                    <dt>Углеводы</dt>
                    <dd><span data-preview-carbohydrates>0</span> г</dd>
                </div>
            </dl>
        </section>

        <p class="field-error" data-calculation-error hidden></p>

        <div class="form-actions">
            <button type="submit">${submitText}</button>
            <a href="/diary?date=${(form.entryDate.toString())!}">Отмена</a>
        </div>
    </form>

    <script src="/js/diary-entry-form.js"></script>
</#macro>
