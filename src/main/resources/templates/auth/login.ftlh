<#import "/layout/base.ftlh" as layout>

<@layout.page title="Вход">
    <section class="auth-page">
        <h1>Вход</h1>

        <#if RequestParameters.error??>
            <div class="alert alert-error">
                Неверный email или пароль.
            </div>
        </#if>

        <#if RequestParameters.logout??>
            <div class="alert alert-success">
                Вы вышли из аккаунта.
            </div>
        </#if>

        <#if RequestParameters.registered??>
            <div class="alert alert-success">
                Регистрация прошла успешно. Теперь войдите.
            </div>
        </#if>

        <form method="post" action="/login" class="auth-form">
            <#include "/layout/csrf.ftlh">

            <div class="form-group">
                <label for="email">Email</label>

                <input
                        id="email"
                        type="email"
                        name="email"
                        required
                        maxlength="254"
                >
            </div>

            <div class="form-group">
                <label for="password">Пароль</label>

                <input
                        id="password"
                        type="password"
                        name="password"
                        required
                >
            </div>

            <button type="submit">Войти</button>
        </form>

        <p>
            Нет аккаунта?
            <a href="/register">Зарегистрироваться</a>
        </p>
    </section>
</@layout.page>