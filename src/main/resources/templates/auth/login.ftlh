<#import "/layout/base.ftlh" as layout>

<@layout.page title="Вход">
    <section class="auth-shell">
        <div class="auth-card">
            <div class="mb-4">
                <span class="badge rounded-pill text-bg-primary-subtle text-primary mb-2">Добро пожаловать</span>
                <h1 class="h3 mb-1">Вход</h1>
                <p class="text-secondary mb-0">Продолжите вести дневник питания.</p>
            </div>

            <#if RequestParameters.error??>
                <div class="alert alert-danger border-0">
                    Неверный email или пароль либо аккаунт заблокирован.
                </div>
            </#if>

            <#if RequestParameters.logout??>
                <div class="alert alert-success border-0">
                    Вы вышли из аккаунта.
                </div>
            </#if>

            <#if RequestParameters.registered??>
                <div class="alert alert-success border-0">
                    Регистрация прошла успешно. Теперь войдите.
                </div>
            </#if>

            <form method="post" action="/login" class="d-grid gap-3">
                <#include "/layout/csrf.ftlh">

                <div>
                    <label class="form-label" for="email">Email</label>

                    <input class="form-control"
                           id="email"
                           type="email"
                           name="email"
                           required
                           maxlength="254"
                    >
                </div>

                <div>
                    <label class="form-label" for="password">Пароль</label>

                    <input class="form-control"
                           id="password"
                           type="password"
                           name="password"
                           required
                    >
                </div>

                <button class="btn btn-primary btn-lg" type="submit">Войти</button>
            </form>

            <p class="mt-4 mb-0 text-secondary">
                Нет аккаунта?
                <a href="/register">Зарегистрироваться</a>
            </p>
        </div>
    </section>
</@layout.page>
