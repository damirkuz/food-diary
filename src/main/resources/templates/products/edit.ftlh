<#import "/layout/base.ftlh" as layout>
<#import "/products/form.ftlh" as productForm>

<@layout.page title="Редактирование продукта">
    <section class="form-shell">
        <div class="app-panel">
            <span class="eyebrow">Справочник</span>
            <h1 class="h3 mb-3">Редактирование продукта</h1>

            <@productForm.productForm action="${springMacroRequestContext.contextPath}/products/${productId}/edit" submitText="Сохранить" />
        </div>
    </section>
</@layout.page>
