<#import "/layout/base.ftlh" as layout>
<#import "/products/form.ftlh" as productForm>

<@layout.page title="Новый продукт">
    <section class="form-shell">
        <div class="app-panel">
            <span class="eyebrow">Справочник</span>
            <h1 class="h3 mb-3">Новый продукт</h1>

            <@productForm.productForm action="/products" submitText="Создать" />
        </div>
    </section>
</@layout.page>
