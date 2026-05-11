<#import "/layout/base.ftlh" as layout>
<#import "/products/form.ftlh" as productForm>

<@layout.page title="Редактирование продукта">
    <section class="form-page">
        <h1>Редактирование продукта</h1>

        <@productForm.productForm action="/products/${productId}/edit" submitText="Сохранить" />
    </section>
</@layout.page>
