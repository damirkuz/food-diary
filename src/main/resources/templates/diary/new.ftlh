<#import "/layout/base.ftlh" as layout>
<#import "/diary/form.ftlh" as diaryForm>

<@layout.page title="Добавить запись">
    <section class="form-page">
        <h1>Добавить запись</h1>

        <#if products?size == 0>
            <p class="empty-state">Сначала добавьте хотя бы один продукт.</p>
            <a class="button-link" href="/products/new">Добавить продукт</a>
        <#else>
            <@diaryForm.foodEntryForm action="/diary/entries" submitText="Добавить запись" />
        </#if>
    </section>
</@layout.page>
