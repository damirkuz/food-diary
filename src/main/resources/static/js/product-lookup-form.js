(function () {
    const form = document.querySelector("[data-product-form]");

    if (!form) {
        return;
    }

    const nameInput = form.querySelector("[data-product-name]");
    const lookupButton = form.querySelector("[data-lookup-product]");
    const status = form.querySelector("[data-lookup-status]");
    const contextPath = document.querySelector('meta[name="_context_path"]')?.content || "";

    const fields = {
        caloriesPer100g: form.querySelector("[data-calories]"),
        proteinsPer100g: form.querySelector("[data-proteins]"),
        fatsPer100g: form.querySelector("[data-fats]"),
        carbohydratesPer100g: form.querySelector("[data-carbohydrates]")
    };

    function setStatus(message, type) {
        status.textContent = message;
        status.dataset.type = type || "";
    }

    function format(value) {
        return Number(value || 0).toFixed(2);
    }

    async function lookup() {
        const name = nameInput.value.trim();

        if (!name) {
            setStatus("Введите название продукта", "error");
            nameInput.focus();
            return;
        }

        lookupButton.disabled = true;
        setStatus("Ищу данные...", "loading");

        try {
            const response = await fetch(`${contextPath}/api/products/nutrition-lookup?name=${encodeURIComponent(name)}`);

            if (response.status === 404) {
                setStatus("Продукт не найден", "error");
                return;
            }

            if (!response.ok) {
                setStatus("Не удалось подтянуть БЖУ", "error");
                return;
            }

            const data = await response.json();
            fields.caloriesPer100g.value = format(data.caloriesPer100g);
            fields.proteinsPer100g.value = format(data.proteinsPer100g);
            fields.fatsPer100g.value = format(data.fatsPer100g);
            fields.carbohydratesPer100g.value = format(data.carbohydratesPer100g);

            if (data.name && !nameInput.value.trim()) {
                nameInput.value = data.name;
            }

            setStatus(`Готово: ${data.name}`, "success");
        } catch (e) {
            setStatus("Не удалось подтянуть БЖУ", "error");
        } finally {
            lookupButton.disabled = false;
        }
    }

    lookupButton.addEventListener("click", lookup);
})();
