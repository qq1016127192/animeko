/*
 * Copyright (C) 2024-2025 OpenAni and contributors.
 *
 * 此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可以在以下链接找到该许可证.
 * Use of this source code is governed by the GNU AGPLv3 license, which can be found at the following link.
 *
 * https://github.com/open-ani/ani/blob/main/LICENSE
 */

package me.him188.ani.app.ui.subject.episode.details.components

import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import me.him188.ani.app.ui.foundation.rememberAsyncHandler
import me.him188.ani.app.ui.foundation.text.ProvideContentColor
import me.him188.ani.app.ui.subject.collection.components.EditCollectionTypeDropDown
import me.him188.ani.app.ui.subject.collection.components.EditableSubjectCollectionTypeState
import me.him188.ani.datasources.api.topic.UnifiedCollectionType

@Composable
fun FavoriteIconButton(
    state: EditableSubjectCollectionTypeState,
    modifier: Modifier = Modifier,
) {
    val tasker = rememberAsyncHandler()
    val presentation by state.presentationFlow.collectAsStateWithLifecycle()

    var showEditCollectionTypeDropDown by rememberSaveable { mutableStateOf(false) }
    val collectionAtLeastWatching = when (presentation.selfCollectionType) {
        UnifiedCollectionType.DOING, UnifiedCollectionType.ON_HOLD, UnifiedCollectionType.DONE -> true
        else -> false
    }

    Box(
        modifier = modifier
            .minimumInteractiveComponentSize()
            .size(IconButtonDefaults.smallContainerSize())
            .clip(CircleShape)
            .background(
                color = if (collectionAtLeastWatching) MaterialTheme.colorScheme.primaryContainer else
                    MaterialTheme.colorScheme.surfaceVariant,
                shape = CircleShape,
            )
            .combinedClickable(
                enabled = true,
                onClick = {
                    tasker.launch {
                        if (collectionAtLeastWatching) {
                            state.setSelfCollectionType(UnifiedCollectionType.NOT_COLLECTED)
                        } else {
                            state.setSelfCollectionType(UnifiedCollectionType.DOING)
                        }
                    }
                },
                onLongClick = { showEditCollectionTypeDropDown = true },
                role = Role.Button,
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(),
            ),
        contentAlignment = Alignment.Center,
    ) {
        ProvideContentColor(
            if (collectionAtLeastWatching) MaterialTheme.colorScheme.onPrimaryContainer else
                MaterialTheme.colorScheme.onSurfaceVariant,
        ) {
            Icon(
                if (collectionAtLeastWatching) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                contentDescription = null,
            )
        }
    }

    EditCollectionTypeDropDown(
        currentType = presentation.selfCollectionType,
        expanded = showEditCollectionTypeDropDown,
        onDismissRequest = { showEditCollectionTypeDropDown = false },
        onClick = {
            showEditCollectionTypeDropDown = false
            tasker.launch {
                state.setSelfCollectionType(it.type)
            }
        },
    )
}