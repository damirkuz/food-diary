<#import "/layout/base.ftlh" as layout>

<@layout.page title="Главная">
    <section class="hero-section rounded-4 overflow-hidden mb-5">
        <div class="hero-content">
            <span class="badge text-bg-light text-primary mb-3">Личный дневник питания</span>
            <h1 class="display-5 fw-bold mb-3">Food Diary</h1>
            <p class="lead mb-4">
                Расчёт суточной нормы калорий и БЖУ, собственный справочник продуктов и дневник питания
                с итогами за каждый день.
            </p>

            <div class="d-flex flex-wrap gap-2">
                <#if isAuthenticated>
                    <a class="btn btn-light btn-lg text-primary" href="${springMacroRequestContext.contextPath}/diary">Открыть дневник</a>
                    <a class="btn btn-outline-light btn-lg" href="${springMacroRequestContext.contextPath}/products">Мои продукты</a>
                <#else>
                    <a class="btn btn-light btn-lg text-primary" href="${springMacroRequestContext.contextPath}/register">Начать работу</a>
                    <a class="btn btn-outline-light btn-lg" href="${springMacroRequestContext.contextPath}/login">Войти</a>
                </#if>
            </div>
        </div>
    </section>

    <section class="row g-4 mb-5">
        <div class="col-md-4">
            <div class="feature-card h-100">
                <div class="feature-mark">01</div>
                <h2 class="h5">Профиль и норма</h2>
                <p class="text-secondary mb-0">
                    Пол, возраст, рост, вес, активность и цель используются для расчёта дневной нормы.
                </p>
            </div>
        </div>
        <div class="col-md-4">
            <div class="feature-card h-100">
                <div class="feature-mark">02</div>
                <h2 class="h5">Продукты и API</h2>
                <p class="text-secondary mb-0">
                    Справочник продуктов можно вести вручную или быстро пополнять через автоматический поиск.
                </p>
            </div>
        </div>
        <div class="col-md-4">
            <div class="feature-card h-100">
                <div class="feature-mark">03</div>
                <h2 class="h5">Дневник и итоги</h2>
                <p class="text-secondary mb-0">
                    Записи группируются по дате, показатели считаются на сервере и сравниваются с нормой.
                </p>
            </div>
        </div>
    </section>

    <section class="app-panel">
        <div class="row align-items-center g-4">
            <div class="col-lg-8">
                <h2 class="h4 mb-2">Всё питание в одном месте</h2>
                <p class="text-secondary mb-0">
                    Заполняйте профиль, сохраняйте продукты, добавляйте приёмы пищи и отслеживайте,
                    насколько дневной рацион совпадает с вашей нормой.
                </p>
            </div>
            <div class="col-lg-4 text-lg-end">
                <a class="btn btn-primary" href="${springMacroRequestContext.contextPath}/profile">Перейти к профилю</a>
            </div>
        </div>
    </section>
</@layout.page>
