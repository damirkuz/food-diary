<#import "/layout/base.ftlh" as layout>
<#import "/diary/form.ftlh" as diaryForm>

<@layout.page title="Редактировать запись">
    <section class="form-page">
        <h1>Редактировать запись</h1>

        <@diaryForm.foodEntryForm action="/diary/entries/${entryId}/edit" submitText="Сохранить запись" />
    </section>
</@layout.page>
