<#import "/layout/base.ftlh" as layout>
<#import "/layout/field-errors.ftlh" as errors>
<#import "/spring.ftl" as spring>

<@layout.page title="Регистрация">
    <section class="auth-shell">
        <div class="auth-card">
            <div class="mb-4">
                <span class="badge rounded-pill text-bg-primary-subtle text-primary mb-2">Новый аккаунт</span>
                <h1 class="h3 mb-1">Регистрация</h1>
                <p class="text-secondary mb-0">Создайте аккаунт для личного дневника.</p>
            </div>

            <form method="post" action="${springMacroRequestContext.contextPath}/register" class="d-grid gap-3">
                <#include "/layout/csrf.ftlh">

                <div>
                    <label class="form-label" for="email">Email</label>

                    <@spring.bind "form.email" />
                    <input class="form-control"
                           id="email"
                           type="email"
                           name="email"
                           value="${spring.status.value!}"
                           required
                           maxlength="254"
                    >

                    <@errors.field "form.email" />
                </div>

                <div>
                    <label class="form-label" for="password">Пароль</label>

                    <input class="form-control"
                           id="password"
                           type="password"
                           name="password"
                           required
                           minlength="6"
                           maxlength="100"
                    >

                    <@errors.field "form.password" />
                </div>

                <button class="btn btn-primary btn-lg" type="submit">Зарегистрироваться</button>
            </form>

            <p class="mt-4 mb-0 text-secondary">
                Уже есть аккаунт?
                <a href="${springMacroRequestContext.contextPath}/login">Войти</a>
            </p>
        </div>
    </section>
</@layout.page>
