<#import "/layout/base.ftlh" as layout>
<#import "/diary/form.ftlh" as diaryForm>

<@layout.page title="Редактировать запись">
    <section class="form-shell">
        <div class="app-panel">
            <span class="eyebrow">Дневник</span>
            <h1 class="h3 mb-3">Редактировать запись</h1>

            <@diaryForm.foodEntryForm action="/diary/entries/${entryId}/edit" submitText="Сохранить запись" />
        </div>
    </section>
</@layout.page>
