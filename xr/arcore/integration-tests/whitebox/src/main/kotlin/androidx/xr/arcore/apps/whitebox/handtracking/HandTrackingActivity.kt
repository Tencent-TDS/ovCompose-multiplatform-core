/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.xr.arcore.apps.whitebox.handtracking

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.xr.arcore.Hand
import androidx.xr.arcore.HandJointType
import androidx.xr.arcore.TrackingState
import androidx.xr.arcore.apps.whitebox.common.BackToMainActivityButton
import androidx.xr.arcore.apps.whitebox.common.SessionLifecycleHelper
import androidx.xr.runtime.Session
import androidx.xr.scenecore.GltfModel
import androidx.xr.scenecore.GltfModelEntity
import androidx.xr.scenecore.Session as JxrCoreSession
import kotlinx.coroutines.guava.await
import kotlinx.coroutines.launch

/** Sample that demonstrates Hand Tracking API usage. */
class HandTrackingActivity : ComponentActivity() {

    private lateinit var session: Session
    private lateinit var sessionHelper: SessionLifecycleHelper

    private lateinit var jxrCoreSession: JxrCoreSession

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Create session and renderers.
        sessionHelper = SessionLifecycleHelper(this)
        session = sessionHelper.session
        lifecycle.addObserver(sessionHelper)

        jxrCoreSession = JxrCoreSession.create(this)
        setContent { MainPanel(session) }
        lifecycleScope.launch {
            val xyzModel = GltfModel.create(jxrCoreSession, "models/xyzArrows.glb").await()

            val leftHandJointEntityMap =
                HandJointType.entries.associateWith {
                    GltfModelEntity.create(jxrCoreSession, xyzModel).also {
                        it.setScale(0.015f)
                        it.setHidden(true)
                    }
                }

            val rightHandJointEntityMap =
                HandJointType.entries.associateWith {
                    GltfModelEntity.create(jxrCoreSession, xyzModel).also {
                        it.setScale(0.015f)
                        it.setHidden(true)
                    }
                }

            launch {
                Hand.left(session)?.state?.collect { leftHandState ->
                    renderHandGizmos(leftHandState, leftHandJointEntityMap)
                }
            }

            launch {
                Hand.right(session)?.state?.collect { rightHandState ->
                    renderHandGizmos(rightHandState, rightHandJointEntityMap)
                }
            }
        }
    }

    private fun renderHandGizmos(
        handState: Hand.State,
        jointEntityMap: Map<HandJointType, GltfModelEntity>,
    ) {
        for ((jointType, gltfModelEntity) in jointEntityMap) {
            if (handState.trackingState == TrackingState.Tracking) {
                // According to the experiment, calling setHidden will significantly
                // increase the latency. Thus, check the hidden state before calling
                // setHidden.
                if (gltfModelEntity.isHidden(false)) {
                    gltfModelEntity.setHidden(false)
                }
                val transformedPose =
                    jxrCoreSession.perceptionSpace.transformPoseTo(
                        handState.handJoints[jointType]!!,
                        jxrCoreSession.activitySpace,
                    )
                gltfModelEntity.setPose(transformedPose)
            } else {
                // According to the experiment, calling setHidden will significantly
                // increase the latency. Thus, check the hidden state before calling
                // setHidden.
                if (gltfModelEntity.isHidden(false)) {
                    return
                }
                gltfModelEntity.setHidden(true)
            }
        }
    }
}

@Composable
fun MainPanel(session: Session) {
    val state by session.state.collectAsStateWithLifecycle()

    val leftHand = Hand.left(session)
    val rightHand = Hand.right(session)

    Column(
        modifier =
            Modifier.background(color = Color.White)
                .fillMaxWidth()
                .fillMaxHeight()
                .padding(horizontal = 20.dp)
    ) {
        BackToMainActivityButton()
        Text(text = "CoreState: ${state.timeMark.toString()}")
        if (leftHand == null || rightHand == null) {
            Text("Hand module is not supported.")
        } else {
            Text("Left hand tracking state: ${leftHand.state.collectAsState().value.trackingState}")
            Text(
                "Right hand tracking state: ${rightHand.state.collectAsState().value.trackingState}"
            )
        }
    }
}
