package ru.kuzdikenov.fooddiary.controller.web

import jakarta.validation.Valid
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.validation.BindingResult
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.ModelAttribute
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.servlet.mvc.support.RedirectAttributes
import ru.kuzdikenov.fooddiary.entity.MealType
import ru.kuzdikenov.fooddiary.form.FoodEntryForm
import ru.kuzdikenov.fooddiary.service.CurrentUserService
import ru.kuzdikenov.fooddiary.service.FoodEntryService
import ru.kuzdikenov.fooddiary.service.NutritionNormService
import ru.kuzdikenov.fooddiary.service.ProductService
import ru.kuzdikenov.fooddiary.service.ProfileService
import java.time.LocalDate

@Controller
@RequestMapping("/diary")
class DiaryWebController (
    private val foodEntryService: FoodEntryService,
    private val currentUserService: CurrentUserService,
    private val productService: ProductService,
    private val profileService: ProfileService,
    private val nutritionNormService: NutritionNormService,
) {

    @GetMapping
    fun index(
        @RequestParam(required = false)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        date: LocalDate?,
        model: Model
    ): String {
        val selectedDate = date ?: LocalDate.now()
        val ownerId = currentUserService.getCurrentUserId()
        val entries = foodEntryService.getEntriesByDate(ownerId, selectedDate)
        val totals = foodEntryService.getTotalsByDate(ownerId, selectedDate)
        val dailyNorm = profileService.findByUserId(ownerId)
            ?.let { nutritionNormService.calculate(it) }
        val highCalorieEntries = foodEntryService.getHighCalorieEntries(ownerId, selectedDate)

        model.addAttribute("selectedDate", selectedDate)
        model.addAttribute("entries", entries)
        model.addAttribute("totals", totals)
        model.addAttribute("dailyNorm", dailyNorm)
        model.addAttribute("highCalorieEntries", highCalorieEntries)
        return "diary/index"
    }

    @GetMapping("/entries/new")
    fun newFoodEntry(
        @RequestParam(required = false)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        date: LocalDate?,
        model: Model
    ): String {
        model.addAttribute("form", FoodEntryForm.forDate(date ?: LocalDate.now()))
        addFormAttributes(model)
        return "diary/new"
    }

    @PostMapping("/entries")
    fun createFoodEntry(
        @Valid @ModelAttribute("form") form: FoodEntryForm,
        bindingResult: BindingResult,
        model: Model,
        redirectAttributes: RedirectAttributes
    ): String {
        if (bindingResult.hasErrors()) {
            addFormAttributes(model)
            return "diary/new"
        }

        val ownerId = currentUserService.getCurrentUserId()
        foodEntryService.createFoodEntry(form.toCommand(), ownerId)
        redirectAttributes.addFlashAttribute("successMessage", "Запись добавлена")

        return "redirect:/diary?date=${form.entryDate}"
    }

    @GetMapping("/entries/{id}/edit")
    fun editFoodEntry(
        @PathVariable id: Long,
        model: Model
    ): String {
        val ownerId = currentUserService.getCurrentUserId()
        val foodEntry = foodEntryService.getFoodEntryById(id, ownerId)

        model.addAttribute("entryId", id)
        model.addAttribute("form", FoodEntryForm.fromFoodEntry(foodEntry))
        addFormAttributes(model)
        return "diary/edit"
    }

    @PostMapping("/entries/{id}/edit")
    fun updateFoodEntry(
        @PathVariable id: Long,
        @Valid @ModelAttribute("form") form: FoodEntryForm,
        bindingResult: BindingResult,
        model: Model,
        redirectAttributes: RedirectAttributes
    ): String {
        if (bindingResult.hasErrors()) {
            model.addAttribute("entryId", id)
            addFormAttributes(model)
            return "diary/edit"
        }

        val ownerId = currentUserService.getCurrentUserId()
        foodEntryService.updateFoodEntry(id, form.toCommand(), ownerId)
        redirectAttributes.addFlashAttribute("successMessage", "Запись обновлена")

        return "redirect:/diary?date=${form.entryDate}"
    }

    @PostMapping("/entries/{id}/delete")
    fun deleteFoodEntry(
        @PathVariable id: Long,
        @RequestParam
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        date: LocalDate,
        redirectAttributes: RedirectAttributes
    ): String {
        val ownerId = currentUserService.getCurrentUserId()
        foodEntryService.deleteFoodEntry(id, ownerId)
        redirectAttributes.addFlashAttribute("successMessage", "Запись удалена")

        return "redirect:/diary?date=$date"
    }

    private fun addFormAttributes(model: Model) {
        val ownerId = currentUserService.getCurrentUserId()
        val products = productService.getCurrentUserProducts(
            ownerId = ownerId,
            search = null,
            page = 0,
            size = 1000,
            sort = "name,asc"
        )

        model.addAttribute("products", products.content)
        model.addAttribute("mealTypes", MealType.entries)
    }


}
