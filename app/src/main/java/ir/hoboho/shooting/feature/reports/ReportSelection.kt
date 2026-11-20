package ir.hoboho.shooting.feature.reports

internal fun selectVisibleWorkday(selectedId: Long?, visibleIds: List<Long>): Long? =
    selectedId?.takeIf { it in visibleIds } ?: visibleIds.firstOrNull()
