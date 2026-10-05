package com.bloomcycle.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bloomcycle.app.domain.chat.ChatIdentityFactory
import com.bloomcycle.app.domain.chat.ChatModerationPolicy
import com.bloomcycle.app.domain.chat.ChatReport
import com.bloomcycle.app.domain.chat.ChatReports
import com.bloomcycle.app.domain.chat.ChatVisibility
import com.bloomcycle.app.domain.chat.ReportReason
import com.bloomcycle.app.domain.chat.SendBlockReason
import com.bloomcycle.app.domain.chat.SendVerdict
import com.bloomcycle.app.domain.cycle.CycleCalculator
import com.bloomcycle.app.domain.cycle.PhaseResolver
import com.bloomcycle.app.domain.model.ChatBuckets
import com.bloomcycle.app.domain.model.ChatMessage
import com.bloomcycle.app.domain.model.ChatScope
import com.bloomcycle.app.domain.model.UserSettings
import com.bloomcycle.app.ui.PageScrollSound
import com.bloomcycle.app.ui.components.EmptyState
import com.bloomcycle.app.ui.components.Gap
import com.bloomcycle.app.ui.components.Hairline
import com.bloomcycle.app.ui.components.SectionHeader
import com.bloomcycle.app.ui.components.SoftCard
import com.bloomcycle.app.ui.rememberAppContainer
import com.bloomcycle.app.ui.theme.Spacing
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

/**
 * Phase 11 — rooms matched to where someone is in their cycle.
 *
 * Two rooms: one global, one for the phase the reader is actually in. Everything that
 * leaves the device is built by [ChatIdentityFactory] — a locally generated pseudonym,
 * a coarse phase bucket and a cycle-length band, never a name or a date (plan.md §7.4).
 *
 * Until the Firestore backend exists the repository is the local Room stub, so the room
 * here is honestly empty rather than seeded with invented people; moderation, reporting
 * and blocking are real and local, which is what lets the safety flows be tested before
 * there is a server to enforce them.
 */
@Composable
fun ChatScreen(modifier: Modifier = Modifier) {
    val container = rememberAppContainer()
    val scope = rememberCoroutineScope()
    val today = remember(container) { container.cycleClock.today() }

    var settings by remember { mutableStateOf<UserSettings?>(null) }
    LaunchedEffect(container) { container.settings.settings.collect { settings = it } }
    val current = settings

    // A pseudonym is required to post, and generating it here means nobody is ever asked
    // to invent a name under social pressure — it is theirs to change afterwards.
    LaunchedEffect(current?.chatDisplayName) {
        val s = current ?: return@LaunchedEffect
        if (s.chatDisplayName.isBlank()) {
            container.settings.update {
                it.copy(chatDisplayName = ChatIdentityFactory.generatedName())
            }
        }
    }

    val periods by container.cycleRepository
        .observePeriods()
        .collectAsStateWithLifecycle(initialValue = emptyList())
    val cycles = remember(periods) { CycleCalculator.buildCycles(periods) }

    val phase = remember(cycles, today, current) {
        current?.let { s ->
            val prediction = CycleCalculator.predict(
                cycles = cycles,
                today = today,
                context = s.cycleContext,
                assumptions = CycleCalculator.Assumptions(defaultCycleLength = s.typicalCycleLength),
            )
            PhaseResolver.resolve(today, cycles, prediction)?.phase
        }
    }
    val averageLength = remember(cycles, current) {
        current?.let { s ->
            CycleCalculator.averageCycleLength(
                cycles,
                CycleCalculator.Assumptions(defaultCycleLength = s.typicalCycleLength),
            )
        }
    }

    var roomScope by rememberSaveable { mutableStateOf(ChatScope.MY_PHASE) }
    val messages by container.chatRepository
        .messages(roomScope)
        .collectAsStateWithLifecycle(initialValue = emptyList())
    val visible = remember(messages, current) {
        ChatVisibility.visible(
            messages = messages,
            reportLog = current?.chatReports ?: emptySet(),
            blockedAuthors = current?.blockedChatAuthors ?: emptySet(),
        )
    }

    val listState = rememberLazyListState()
    LaunchedEffect(visible.size, roomScope) {
        if (visible.isNotEmpty()) listState.animateScrollToItem(visible.lastIndex)
    }

    var draft by remember { mutableStateOf("") }
    var lastSentAt by remember { mutableStateOf<Instant?>(null) }
    var nowTick by remember { mutableStateOf(Instant.now()) }
    val verdict = remember(draft, lastSentAt, nowTick) {
        ChatModerationPolicy.check(draft, lastSentAt, nowTick)
    }
    // Only ticks while the rate limit is the thing standing between the user and sending.
    LaunchedEffect(verdict.blocked) {
        if (verdict.blocked == SendBlockReason.TOO_FAST) {
            while (true) {
                delay(1_000)
                nowTick = Instant.now()
            }
        }
    }

    val send: () -> Unit = {
        if (verdict.allowed && current != null) {
            val identity = ChatIdentityFactory.create(
                displayName = current.chatDisplayName,
                phase = phase,
                averageCycleLength = averageLength,
            )
            val payload = ChatModerationPolicy.sanitize(draft)
            scope.launch {
                container.chatRepository.send(payload, roomScope, identity)
                lastSentAt = Instant.now()
                draft = ""
            }
        }
    }

    var reportTarget by remember { mutableStateOf<ChatMessage?>(null) }
    var editingName by remember { mutableStateOf(false) }

    val fileReport: (ChatMessage, ReportReason) -> Unit = { message, reason ->
        reportTarget = null
        scope.launch {
            container.settings.update { s ->
                s.copy(
                    chatReports = ChatReports.add(
                        s.chatReports,
                        ChatReport(message.id, message.authorName, reason, Instant.now()),
                    ),
                )
            }
        }
    }
    val blockAuthor: (String) -> Unit = { author ->
        reportTarget = null
        scope.launch {
            container.settings.update { s ->
                s.copy(blockedChatAuthors = s.blockedChatAuthors + author)
            }
        }
    }

    Scaffold(
        modifier = modifier,
        bottomBar = {
            Composer(
                draft = draft,
                verdict = verdict,
                onDraftChange = { draft = it },
                onSend = send,
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            Column(modifier = Modifier.padding(horizontal = Spacing.screen)) {
                Gap(Spacing.md)
                SectionHeader(
                    eyebrow = "Chat",
                    title = "Talk it through",
                    trailing = {
                        IconButton(onClick = { editingName = true }) {
                            Icon(Icons.Outlined.Edit, contentDescription = "Change your chat name")
                        }
                    },
                )
                Gap(Spacing.xs)
                Text(
                    text = "Posting as ${current?.chatDisplayName.orEmpty().ifBlank { "…" }}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.semantics {
                        contentDescription = "Posting as ${current?.chatDisplayName.orEmpty()}"
                    },
                )
                Gap(Spacing.sm)
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                    RoomPill(
                        label = ChatScope.GLOBAL.label,
                        selected = roomScope == ChatScope.GLOBAL,
                        onClick = { roomScope = ChatScope.GLOBAL },
                    )
                    RoomPill(
                        label = ChatScope.MY_PHASE.label,
                        selected = roomScope == ChatScope.MY_PHASE,
                        onClick = { roomScope = ChatScope.MY_PHASE },
                    )
                }
                Gap(Spacing.xs)
                Text(
                    text = when (roomScope) {
                        ChatScope.GLOBAL -> "Everyone, whichever phase they are in."
                        ChatScope.MY_PHASE -> "People ${ChatBuckets.forPhase(phase)} right now."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Gap(Spacing.xxs)
                Text(
                    text = "A pseudonym and a coarse phase label only — never your name, " +
                        "your dates or your birth date.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Gap(Spacing.sm)
            }

            when {
                messages.isEmpty() -> {
                    EmptyState(
                        title = "The room is quiet",
                        body = "Nobody has posted here yet. Be the first — or switch rooms above.",
                        glyph = "◌",
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(Spacing.screen),
                    )
                }
                visible.isEmpty() -> {
                    EmptyState(
                        title = "Nothing to show",
                        body = "Everything here came from people you have reported or blocked.",
                        glyph = "◌",
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(Spacing.screen),
                    )
                }
                else -> {
                    PageScrollSound(listState)
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(Spacing.screen),
                        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
                    ) {
                        items(visible, key = { it.id }) { message ->
                            MessageRow(
                                message = message,
                                onReport = { reportTarget = message },
                            )
                        }
                    }
                }
            }
        }
    }

    reportTarget?.let { target ->
        ReportDialog(
            authorName = target.authorName,
            onReason = { fileReport(target, it) },
            onBlock = { blockAuthor(target.authorName) },
            onDismiss = { reportTarget = null },
        )
    }

    if (editingName) {
        NameDialog(
            initial = current?.chatDisplayName.orEmpty(),
            onSave = { chosen ->
                editingName = false
                scope.launch {
                    container.settings.update { s ->
                        s.copy(
                            chatDisplayName = ChatIdentityFactory.sanitizeDisplayName(chosen)
                                .ifBlank { ChatIdentityFactory.generatedName() },
                        )
                    }
                }
            },
            onDismiss = { editingName = false },
        )
    }
}

@Composable
private fun RoomPill(label: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier.semantics {
            contentDescription = if (selected) "$label, selected room" else "$label room"
        },
        shape = MaterialTheme.shapes.extraLarge,
        color = if (selected) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceVariant
        },
        border = BorderStroke(
            1.dp,
            if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
        ),
    ) {
        Text(
            // The tick carries the selection in greyscale and through TalkBack, so the
            // choice never depends on colour alone.
            text = if (selected) "✓ $label" else label,
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(horizontal = Spacing.md, vertical = Spacing.xs),
        )
    }
}

@Composable
private fun MessageRow(message: ChatMessage, onReport: () -> Unit) {
    val time = remember(message.createdAt) {
        message.createdAt.atZone(ZoneId.systemDefault()).format(TimeFormat)
    }
    SoftCard(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(Spacing.sm),
    ) {
        Row(verticalAlignment = Alignment.Top) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = message.authorName,
                        style = MaterialTheme.typography.labelLarge,
                    )
                    if (message.isOwn) {
                        Spacer(Modifier.width(Spacing.xs))
                        Text(
                            text = "you",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Spacer(Modifier.width(Spacing.sm))
                    Text(
                        text = time,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Gap(Spacing.xxxs)
                Text(text = message.text, style = MaterialTheme.typography.bodyMedium)
            }
            if (!message.isOwn) {
                IconButton(onClick = onReport) {
                    Icon(
                        Icons.Outlined.Flag,
                        contentDescription = "Report this message from ${message.authorName}",
                    )
                }
            }
        }
    }
}

@Composable
private fun Composer(
    draft: String,
    verdict: SendVerdict,
    onDraftChange: (String) -> Unit,
    onSend: () -> Unit,
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 3.dp,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.screen)
                .imePadding(),
        ) {
            if (!verdict.allowed && verdict.blocked != SendBlockReason.EMPTY) {
                Text(
                    text = verdict.message,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error,
                )
                Gap(Spacing.xxs)
            }
            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
            ) {
                OutlinedTextField(
                    value = draft,
                    onValueChange = onDraftChange,
                    modifier = Modifier.weight(1f),
                    label = { Text("Message") },
                    maxLines = 4,
                    isError = verdict.blocked == SendBlockReason.TOO_LONG ||
                        verdict.blocked == SendBlockReason.INAPPROPRIATE,
                )
                IconButton(
                    onClick = onSend,
                    enabled = verdict.allowed && draft.isNotBlank(),
                    modifier = Modifier.padding(bottom = Spacing.xs),
                ) {
                    Icon(
                        Icons.AutoMirrored.Outlined.Send,
                        contentDescription = "Send message",
                    )
                }
            }
            Gap(Spacing.xxs)
            Text(
                text = "${draft.length}/${ChatModerationPolicy.MAX_LENGTH}",
                style = MaterialTheme.typography.labelSmall,
                color = if (draft.length > ChatModerationPolicy.MAX_LENGTH) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
        }
    }
}

@Composable
private fun ReportDialog(
    authorName: String,
    onReason: (ReportReason) -> Unit,
    onBlock: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Report a message") },
        text = {
            Column {
                Text(
                    text = "It disappears from your room straight away and is kept for " +
                        "review. You can also block $authorName entirely.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Gap(Spacing.sm)
                ReportReason.entries.forEach { reason ->
                    TextButton(
                        onClick = { onReason(reason) },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            text = reason.label,
                            modifier = Modifier.fillMaxWidth(),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
                Hairline()
                TextButton(
                    onClick = onBlock,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = "Block $authorName",
                        modifier = Modifier.fillMaxWidth(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

@Composable
private fun NameDialog(
    initial: String,
    onSave: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var nameDraft by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Your chat name") },
        text = {
            Column {
                Text(
                    text = "A pseudonym, made up on this device. Nobody in the rooms " +
                        "ever sees your real name.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Gap(Spacing.sm)
                OutlinedTextField(
                    value = nameDraft,
                    onValueChange = { nameDraft = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Display name") },
                    singleLine = true,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(nameDraft) }) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

private val TimeFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
