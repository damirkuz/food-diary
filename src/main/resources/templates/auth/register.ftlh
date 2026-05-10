<#import "/layout/base.ftlh" as layout>
<#import "/layout/field-errors.ftlh" as errors>
<#import "/spring.ftl" as spring>

<@layout.page title="Регистрация">
    <section class="auth-page">
        <h1>Регистрация</h1>

        <form method="post" action="/register" class="auth-form">
            <#include "/layout/csrf.ftlh">

            <div class="form-group">
                <label for="email">Email</label>

                <@spring.bind "form.email" />
                <input
                        id="email"
                        type="email"
                        name="email"
                        value="${spring.status.value!}"
                        required
                        maxlength="254"
                >

                <@errors.field "form.email" />
            </div>

            <div class="form-group">
                <label for="password">Пароль</label>

                <input
                        id="password"
                        type="password"
                        name="password"
                        required
                        minlength="6"
                        maxlength="100"
                >

                <@errors.field "form.password" />
            </div>

            <button type="submit">Зарегистрироваться</button>
        </form>

        <p>
            Уже есть аккаунт?
            <a href="/login">Войти</a>
        </p>
    </section>
</@layout.page>
