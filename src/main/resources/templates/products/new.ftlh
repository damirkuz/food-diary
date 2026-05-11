<#import "/layout/base.ftlh" as layout>
<#import "/products/form.ftlh" as productForm>

<@layout.page title="Новый продукт">
    <section class="form-page">
        <h1>Новый продукт</h1>

        <@productForm.productForm action="/products" submitText="Создать" />
    </section>
</@layout.page>
