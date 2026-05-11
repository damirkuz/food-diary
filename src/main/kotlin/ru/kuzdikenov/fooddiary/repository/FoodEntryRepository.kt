package ru.kuzdikenov.fooddiary.repository

import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import ru.kuzdikenov.fooddiary.dto.DiaryTotals
import ru.kuzdikenov.fooddiary.entity.FoodEntryEntity
import java.time.LocalDate

interface FoodEntryRepository : JpaRepository<FoodEntryEntity, Long> {

    @EntityGraph(attributePaths = ["product"])
    fun findAllByUserIdAndEntryDateOrderByMealTypeAscCreatedAtAsc(
        userId: Long,
        entryDate: LocalDate
    ): List<FoodEntryEntity>

    @Query(
        """
        select new ru.kuzdikenov.fooddiary.dto.DiaryTotals(
            coalesce(sum(e.calories), 0.0),
            coalesce(sum(e.proteins), 0.0),
            coalesce(sum(e.fats), 0.0),
            coalesce(sum(e.carbohydrates), 0.0)
        )
        from FoodEntryEntity e
        where e.user.id = :userId and e.entryDate = :entryDate
        """
    )
    fun sumNutritionByUserIdAndEntryDate(
        @Param("userId") userId: Long,
        @Param("entryDate") entryDate: LocalDate
    ): DiaryTotals

    @EntityGraph(attributePaths = ["product"])
    @Query(
        """
        select e
        from FoodEntryEntity e
        where e.user.id = :userId
            and e.entryDate = :entryDate
            and e.calories > (
                select avg(dayEntry.calories)
                from FoodEntryEntity dayEntry
                where dayEntry.user.id = :userId and dayEntry.entryDate = :entryDate
            )
        order by e.calories desc
        """
    )
    fun findHighCalorieEntriesByUserIdAndEntryDate(
        @Param("userId") userId: Long,
        @Param("entryDate") entryDate: LocalDate
    ): List<FoodEntryEntity>
}
