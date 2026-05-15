<#import "/layout/base.ftlh" as layout>
<#import "/products/form.ftlh" as productForm>

<@layout.page title="Админка · Редактирование продукта">
    <section class="form-shell">
        <div class="app-panel">
            <span class="eyebrow">Администрирование</span>
            <h1 class="h3 mb-1">Редактирование продукта</h1>
            <p class="text-secondary mb-3">Владелец: ${ownerEmail}</p>

            <@productForm.productForm
                action="${springMacroRequestContext.contextPath}/admin/products/${productId}/edit"
                submitText="Сохранить"
                cancelUrl="/admin/products" />
        </div>
    </section>
</@layout.page>
