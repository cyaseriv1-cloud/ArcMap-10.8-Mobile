package com.terraaxis.tools.query

import com.terraaxis.core.model.Feature
import com.terraaxis.core.model.VectorLayer

object AttributeQuery {

    /**
     * Executes ArcMap "Select by Attributes":
     * Selects features matching the SQL-like condition.
     * Supports simple expressions:
     *   FIELD = 'VALUE'
     *   FIELD != 'VALUE'
     *   FIELD > NUMBER
     *   FIELD >= NUMBER
     *   FIELD < NUMBER
     *   FIELD <= NUMBER
     *   FIELD LIKE '%VAL%'
     * And compound expressions combined by AND / OR.
     */
    fun selectByAttributes(
        layer: VectorLayer,
        whereClause: String,
        selectionMode: SelectionMode = SelectionMode.NEW_SELECTION
    ): Int {
        val trimmed = whereClause.trim()
        if (trimmed.isEmpty()) {
            if (selectionMode == SelectionMode.NEW_SELECTION) layer.clearSelection()
            return 0
        }

        val matchingFeatures = layer.features.filter { evaluateExpression(it, trimmed) }
        val matchingIds = matchingFeatures.map { it.id }.toSet()

        when (selectionMode) {
            SelectionMode.NEW_SELECTION -> {
                layer.clearSelection()
                layer.selectedFeatureIds.addAll(matchingIds)
            }
            SelectionMode.ADD_TO_SELECTION -> {
                layer.selectedFeatureIds.addAll(matchingIds)
            }
            SelectionMode.REMOVE_FROM_SELECTION -> {
                layer.selectedFeatureIds.removeAll(matchingIds)
            }
            SelectionMode.SELECT_SUBSET -> {
                layer.selectedFeatureIds.retainAll(matchingIds)
            }
        }

        return layer.selectedFeatureIds.size
    }

    private fun evaluateExpression(feature: Feature, expr: String): Boolean {
        // Check for OR operator
        if (expr.contains(" OR ", ignoreCase = true)) {
            val parts = expr.split(Regex("(?i)\\s+OR\\s+"))
            return parts.any { evaluateExpression(feature, it) }
        }

        // Check for AND operator
        if (expr.contains(" AND ", ignoreCase = true)) {
            val parts = expr.split(Regex("(?i)\\s+AND\\s+"))
            return parts.all { evaluateExpression(feature, it) }
        }

        return evaluateSimpleCondition(feature, expr.trim())
    }

    private fun evaluateSimpleCondition(feature: Feature, condition: String): Boolean {
        val opRegex = Regex("(<=|>=|!=|=|<|>|(?i)\\s+LIKE\\s+)")
        val match = opRegex.find(condition) ?: return false

        val op = match.value.trim().uppercase()
        val left = condition.substring(0, match.range.first).trim().removeSurrounding("[", "]").removeSurrounding("\"", "\"")
        val right = condition.substring(match.range.last + 1).trim().removeSurrounding("'", "'").removeSurrounding("\"", "\"")

        val rawAttr = feature.attributes.entries.find { it.key.equals(left, ignoreCase = true) }?.value

        val numAttr = (rawAttr as? Number)?.toDouble() ?: rawAttr?.toString()?.toDoubleOrNull()
        val numRight = right.toDoubleOrNull()

        if (numAttr != null && numRight != null) {
            return when (op) {
                "=" -> abs(numAttr - numRight) < 1e-9
                "!=" -> abs(numAttr - numRight) >= 1e-9
                ">" -> numAttr > numRight
                ">=" -> numAttr >= numRight
                "<" -> numAttr < numRight
                "<=" -> numAttr <= numRight
                else -> false
            }
        }

        val strAttr = rawAttr?.toString() ?: ""
        return when (op) {
            "=" -> strAttr.equals(right, ignoreCase = true)
            "!=" -> !strAttr.equals(right, ignoreCase = true)
            "LIKE" -> {
                val pattern = right.replace("%", ".*").replace("_", ".")
                Regex("(?i)^$pattern$").matches(strAttr)
            }
            else -> false
        }
    }

    private fun abs(d: Double): Double = kotlin.math.abs(d)
}

enum class SelectionMode {
    NEW_SELECTION,
    ADD_TO_SELECTION,
    REMOVE_FROM_SELECTION,
    SELECT_SUBSET
}
