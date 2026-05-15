(function () {
    const form = document.querySelector("[data-food-entry-form]");

    if (!form) {
        return;
    }

    const productInput = form.querySelector("[data-product-id]");
    const gramsInput = form.querySelector("[data-grams]");
    const preview = form.querySelector("[data-nutrition-preview]");
    const error = form.querySelector("[data-calculation-error]");
    const csrfToken = document.querySelector('meta[name="_csrf"]')?.content;
    const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.content;
    const contextPath = document.querySelector('meta[name="_context_path"]')?.content || "";

    const fields = {
        calories: form.querySelector("[data-preview-calories]"),
        proteins: form.querySelector("[data-preview-proteins]"),
        fats: form.querySelector("[data-preview-fats]"),
        carbohydrates: form.querySelector("[data-preview-carbohydrates]")
    };

    function format(value) {
        return Number(value || 0).toLocaleString("ru-RU", {
            maximumFractionDigits: 1
        });
    }

    function hidePreview(message) {
        preview.hidden = true;

        if (message) {
            error.textContent = message;
            error.hidden = false;
        } else {
            error.hidden = true;
        }
    }

    async function calculate() {
        const productId = Number(productInput.value);
        const grams = Number(gramsInput.value);

        if (!productId || !grams || grams <= 0) {
            hidePreview();
            return;
        }

        const headers = {
            "Content-Type": "application/json"
        };

        if (csrfToken && csrfHeader) {
            headers[csrfHeader] = csrfToken;
        }

        try {
            const response = await fetch(`${contextPath}/api/diary/entries/calculate`, {
                method: "POST",
                headers,
                body: JSON.stringify({ productId, grams })
            });

            if (!response.ok) {
                hidePreview("Не удалось рассчитать БЖУ");
                return;
            }

            const data = await response.json();
            fields.calories.textContent = format(data.calculatedCalories);
            fields.proteins.textContent = format(data.calculatedProteins);
            fields.fats.textContent = format(data.calculatedFats);
            fields.carbohydrates.textContent = format(data.calculatedCarbohydrates);
            error.hidden = true;
            preview.hidden = false;
        } catch (e) {
            hidePreview("Не удалось рассчитать БЖУ");
        }
    }

    productInput.addEventListener("change", calculate);
    gramsInput.addEventListener("input", calculate);
    calculate();
})();
