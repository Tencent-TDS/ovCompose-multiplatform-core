/*
 * Copyright 2023 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.compose.material3.adaptive.layout

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * An opinionated implementation of [ThreePaneScaffold] following Material guidelines that displays
 * the provided three panes in a canonical [list-detail layout](
 * https://m3.material.io/foundations/layout/canonical-layouts/list-detail).
 *
 * This overload takes a [ThreePaneScaffoldValue] describing the adapted value of each pane within
 * the scaffold.
 *
 * See usage samples at:
 *
 * @sample androidx.compose.material3.adaptive.samples.ListDetailPaneScaffoldSample
 * @sample androidx.compose.material3.adaptive.samples.ListDetailPaneScaffoldSampleWithExtraPane
 * @sample androidx.compose.material3.adaptive.samples.ListDetailPaneScaffoldWithNavigationSample
 * @param directive The top-level directives about how the scaffold should arrange its panes.
 * @param value The current adapted value of the scaffold, which indicates how each pane of the
 *   scaffold is adapted.
 * @param listPane the list pane of the scaffold, which is supposed to hold a list of item summaries
 *   that can be selected from, for example, the inbox mail list of a mail app. See
 *   [ListDetailPaneScaffoldRole.List].
 * @param detailPane the detail pane of the scaffold, which is supposed to hold the detailed info of
 *   a selected item, for example, the mail content currently being viewed. See
 *   [ListDetailPaneScaffoldRole.Detail].
 * @param modifier [Modifier] of the scaffold layout.
 * @param extraPane the extra pane of the scaffold, which is supposed to hold any supplementary info
 *   besides the list and the detail panes, for example, a task list or a mini-calendar view of a
 *   mail app. See [ListDetailPaneScaffoldRole.Extra].
 * @param paneMotions The specified motion of the panes. By default the value will be calculated by
 *   [calculateListDetailPaneScaffoldMotion] according to the target [ThreePaneScaffoldValue].
 * @param paneExpansionDragHandle provide a custom pane expansion drag handle to allow users to
 *   resize panes and change the pane expansion state by dragging. This is `null` by default, which
 *   renders no drag handle. Even there's no drag handle, you can still change pane size directly
 *   via modifying [paneExpansionState].
 * @param paneExpansionState the state object of pane expansion.
 */
@ExperimentalMaterial3AdaptiveApi
@Composable
fun ListDetailPaneScaffold(
    directive: PaneScaffoldDirective,
    value: ThreePaneScaffoldValue,
    listPane: @Composable ThreePaneScaffoldPaneScope.() -> Unit,
    detailPane: @Composable ThreePaneScaffoldPaneScope.() -> Unit,
    modifier: Modifier = Modifier,
    extraPane: (@Composable ThreePaneScaffoldPaneScope.() -> Unit)? = null,
    paneMotions: ThreePaneMotion = calculateListDetailPaneScaffoldMotion(value),
    paneExpansionDragHandle: (@Composable ThreePaneScaffoldScope.(PaneExpansionState) -> Unit)? =
        null,
    paneExpansionState: PaneExpansionState = rememberPaneExpansionState(value),
) {
    ThreePaneScaffold(
        modifier = modifier.fillMaxSize(),
        scaffoldDirective = directive,
        scaffoldValue = value,
        paneOrder = ListDetailPaneScaffoldDefaults.PaneOrder,
        paneMotions = paneMotions,
        secondaryPane = listPane,
        tertiaryPane = extraPane,
        paneExpansionDragHandle = paneExpansionDragHandle,
        paneExpansionState = paneExpansionState,
        primaryPane = detailPane
    )
}

/**
 * An opinionated implementation of [ThreePaneScaffold] following Material guidelines that displays
 * the provided three panes in a canonical [list-detail layout](
 * https://m3.material.io/foundations/layout/canonical-layouts/list-detail).
 *
 * This overload takes a [ThreePaneScaffoldState] describing the current [ThreePaneScaffoldValue]
 * and any pane transitions or animations in progress.
 *
 * @param directive The top-level directives about how the scaffold should arrange its panes.
 * @param scaffoldState The current state of the scaffold, containing information about the adapted
 *   value of each pane of the scaffold and the transitions/animations in progress.
 * @param listPane the list pane of the scaffold, which is supposed to hold a list of item summaries
 *   that can be selected from, for example, the inbox mail list of a mail app. See
 *   [ListDetailPaneScaffoldRole.List].
 * @param detailPane the detail pane of the scaffold, which is supposed to hold the detailed info of
 *   a selected item, for example, the mail content currently being viewed. See
 *   [ListDetailPaneScaffoldRole.Detail].
 * @param modifier [Modifier] of the scaffold layout.
 * @param extraPane the extra pane of the scaffold, which is supposed to hold any supplementary info
 *   besides the list and the detail panes, for example, a task list or a mini-calendar view of a
 *   mail app. See [ListDetailPaneScaffoldRole.Extra].
 * @param paneMotions The specified motion of the panes. By default the value will be calculated by
 *   [calculateListDetailPaneScaffoldMotion] according to the target [ThreePaneScaffoldValue].
 * @param paneExpansionDragHandle provide a custom pane expansion drag handle to allow users to
 *   resize panes and change the pane expansion state by dragging. This is `null` by default, which
 *   renders no drag handle. Even there's no drag handle, you can still change pane size directly
 *   via modifying [paneExpansionState].
 * @param paneExpansionState the state object of pane expansion.
 */
@ExperimentalMaterial3AdaptiveApi
@Composable
fun ListDetailPaneScaffold(
    directive: PaneScaffoldDirective,
    scaffoldState: ThreePaneScaffoldState,
    listPane: @Composable ThreePaneScaffoldPaneScope.() -> Unit,
    detailPane: @Composable ThreePaneScaffoldPaneScope.() -> Unit,
    modifier: Modifier = Modifier,
    extraPane: (@Composable ThreePaneScaffoldPaneScope.() -> Unit)? = null,
    paneMotions: ThreePaneMotion = scaffoldState.calculateListDetailPaneScaffoldMotion(),
    paneExpansionDragHandle: (@Composable ThreePaneScaffoldScope.(PaneExpansionState) -> Unit)? =
        null,
    paneExpansionState: PaneExpansionState = rememberPaneExpansionState(scaffoldState.targetState),
) {
    ThreePaneScaffold(
        modifier = modifier.fillMaxSize(),
        scaffoldDirective = directive,
        scaffoldState = scaffoldState,
        paneOrder = ListDetailPaneScaffoldDefaults.PaneOrder,
        paneMotions = paneMotions,
        secondaryPane = listPane,
        tertiaryPane = extraPane,
        paneExpansionDragHandle = paneExpansionDragHandle,
        paneExpansionState = paneExpansionState,
        primaryPane = detailPane
    )
}

/** Provides default values of [ListDetailPaneScaffold]. */
@ExperimentalMaterial3AdaptiveApi
object ListDetailPaneScaffoldDefaults {
    /**
     * Creates a default [ThreePaneScaffoldAdaptStrategies] for [ListDetailPaneScaffold].
     *
     * @param detailPaneAdaptStrategy the adapt strategy of the primary pane
     * @param listPaneAdaptStrategy the adapt strategy of the secondary pane
     * @param extraPaneAdaptStrategy the adapt strategy of the tertiary pane
     */
    fun adaptStrategies(
        detailPaneAdaptStrategy: AdaptStrategy = AdaptStrategy.Hide,
        listPaneAdaptStrategy: AdaptStrategy = AdaptStrategy.Hide,
        extraPaneAdaptStrategy: AdaptStrategy = AdaptStrategy.Hide,
    ): ThreePaneScaffoldAdaptStrategies =
        ThreePaneScaffoldAdaptStrategies(
            detailPaneAdaptStrategy,
            listPaneAdaptStrategy,
            extraPaneAdaptStrategy
        )

    /**
     * Denotes [ThreePaneScaffold] to use the list-detail pane-order to arrange its panes
     * horizontally, which allocates panes in the order of secondary, primary, and tertiary from
     * start to end.
     */
    internal val PaneOrder =
        ThreePaneScaffoldHorizontalOrder(
            ThreePaneScaffoldRole.Secondary,
            ThreePaneScaffoldRole.Primary,
            ThreePaneScaffoldRole.Tertiary
        )
}

/**
 * The set of the available pane roles of [ListDetailPaneScaffold]. Those roles map to their
 * corresponding [ThreePaneScaffoldRole], which is a generic role definition across all types of
 * three pane scaffolds. We suggest you to use the values defined here instead of the raw
 * [ThreePaneScaffoldRole] under the context of [ListDetailPaneScaffold] for better code clarity.
 */
object ListDetailPaneScaffoldRole {
    /**
     * The list pane of [ListDetailPaneScaffold], which is supposed to hold a list of item summaries
     * that can be selected from, for example, the inbox mail list of a mail app. It maps to
     * [ThreePaneScaffoldRole.Secondary].
     */
    val List = ThreePaneScaffoldRole.Secondary

    /**
     * The detail pane of [ListDetailPaneScaffold], which is supposed to hold the detailed info of a
     * selected item, for example, the mail content currently being viewed. It maps to
     * [ThreePaneScaffoldRole.Primary].
     */
    val Detail = ThreePaneScaffoldRole.Primary

    /**
     * The extra pane of [ListDetailPaneScaffold], which is supposed to hold any supplementary info
     * besides the list and the detail panes, for example, a task list or a mini-calendar view of a
     * mail app. It maps to [ThreePaneScaffoldRole.Tertiary].
     */
    val Extra = ThreePaneScaffoldRole.Tertiary
}
