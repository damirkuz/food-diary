<#import "/layout/base.ftlh" as layout>
<#import "/diary/form.ftlh" as diaryForm>

<@layout.page title="Добавить запись">
    <section class="form-shell">
        <div class="app-panel">
            <span class="eyebrow">Дневник</span>
            <h1 class="h3 mb-3">Добавить запись</h1>

            <#if products?size == 0>
                <div class="empty-state">
                    <h2 class="h5">Нет продуктов</h2>
                    <p class="text-secondary mb-3">Сначала добавьте хотя бы один продукт.</p>
                    <a class="btn btn-primary" href="/products/new">Добавить продукт</a>
                </div>
            <#else>
                <@diaryForm.foodEntryForm action="/diary/entries" submitText="Добавить запись" />
            </#if>
        </div>
    </section>
</@layout.page>
