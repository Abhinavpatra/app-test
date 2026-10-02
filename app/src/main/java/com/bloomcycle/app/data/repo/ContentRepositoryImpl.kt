package com.bloomcycle.app.data.repo

import com.bloomcycle.app.domain.content.ContentLibrary
import com.bloomcycle.app.domain.model.ContentCategory
import com.bloomcycle.app.domain.model.ContentItem
import com.bloomcycle.app.domain.model.PhaseType
import com.bloomcycle.app.domain.repository.ContentRepository

class ContentRepositoryImpl : ContentRepository {

    override fun itemsFor(phase: PhaseType?): List<ContentItem> =
        ContentLibrary.items.filter { it.phaseTag == null || it.phaseTag == phase }

    override fun byCategory(phase: PhaseType?, category: ContentCategory): List<ContentItem> =
        itemsFor(phase).filter { it.category == category }
}
