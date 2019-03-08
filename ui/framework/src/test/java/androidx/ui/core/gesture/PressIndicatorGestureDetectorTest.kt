/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.ui.core.gesture

import androidx.ui.core.pointerinput.PointerEventPass
import androidx.ui.core.pointerinput.consumeDownChange
import androidx.ui.engine.geometry.Offset
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.never
import com.nhaarman.mockitokotlin2.verify
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class PressIndicatorGestureDetectorTest {

    private lateinit var recognizer: PressIndicatorGestureRecognizer
    private val down = down(0, 0f)
    private val downConsumed = down.consumeDownChange()
    private val move = down.moveTo(100f)
    private val moveConsumed = move.consume(dx = 1f)
    private val up = move.up()
    private val upConsumed = up.consumeDownChange()
    private val upAfterMove = move.up()

    @Before
    fun setup() {
        recognizer = PressIndicatorGestureRecognizer()
        recognizer.onStart = mock()
        recognizer.onStop = mock()
        recognizer.onCancel = mock()
    }

    @Test
    fun pointerInputHandler_down_onStartCalledOnce() {
        recognizer.pointerInputHandler.invokeOverAllPasses(down)
        verify(recognizer.onStart!!).invoke(any())
    }

    @Test
    fun pointerInputHandler_downDown_onStartCalledOnce() {
        val down0 = down(0)
        val down1 = down(1)
        recognizer.pointerInputHandler.invokeOverAllPasses(down0)
        recognizer.pointerInputHandler.invokeOverAllPasses(down1)
        verify(recognizer.onStart!!).invoke(any())
    }

    @Test
    fun pointerInputHandler_downDownUpDown_onStartCalledOnce() {
        val down0 = down(0)
        val down1 = down(1)
        val up0 = down1.up()
        recognizer.pointerInputHandler.invokeOverAllPasses(down0)
        recognizer.pointerInputHandler.invokeOverAllPasses(down1)
        recognizer.pointerInputHandler.invokeOverAllPasses(up0)
        recognizer.pointerInputHandler.invokeOverAllPasses(down0)
        verify(recognizer.onStart!!).invoke(any())
    }

    @Test
    fun pointerInputHandler_downChangeConsumedDuringPostUp() {
        var pointerEventChange = down
        pointerEventChange = recognizer.pointerInputHandler.invokeOverPasses(
            pointerEventChange,
            PointerEventPass.InitialDown,
            PointerEventPass.PreUp,
            PointerEventPass.PreDown)
        assertThat(pointerEventChange.consumed.downChange, `is`(false))

        pointerEventChange = recognizer.pointerInputHandler.invoke(
            pointerEventChange,
            PointerEventPass.PostUp)
        assertThat(pointerEventChange.consumed.downChange, `is`(true))
    }

    @Test
    fun pointerInputHandler_downConsumed_onStartNotCalled() {
        recognizer.pointerInputHandler.invokeOverAllPasses(down.consumeDownChange())
        verify(recognizer.onStart!!, never()).invoke(any())
    }

    @Test
    fun pointerInputHandler_downUpConsumed_onStopCalledOnce() {
        recognizer.pointerInputHandler.invokeOverAllPasses(down)
        recognizer.pointerInputHandler.invokeOverAllPasses(upConsumed)
        verify(recognizer.onStop!!).invoke()
    }

    @Test
    fun pointerInputHandler_downMoveUp_onStopCalledOnce() {
        recognizer.pointerInputHandler.invokeOverAllPasses(down)
        recognizer.pointerInputHandler.invokeOverAllPasses(move)
        recognizer.pointerInputHandler.invokeOverAllPasses(upAfterMove)
        verify(recognizer.onStop!!).invoke()
    }

    @Test
    fun pointerInputHandler_downDownUpUp_onStopCalledOnce() {
        val down0 = down(0)
        val down1 = down(1)
        val up0 = down0.up()
        val up1 = down1.up()
        recognizer.pointerInputHandler.invokeOverAllPasses(down0)
        recognizer.pointerInputHandler.invokeOverAllPasses(down1)
        recognizer.pointerInputHandler.invokeOverAllPasses(up0)
        recognizer.pointerInputHandler.invokeOverAllPasses(up1)
        verify(recognizer.onStop!!).invoke()
    }

    @Test
    fun pointerInputHandler_downMoveConsumedUp_onStopNotCalled() {
        recognizer.pointerInputHandler.invokeOverAllPasses(down)
        recognizer.pointerInputHandler.invokeOverAllPasses(moveConsumed)
        recognizer.pointerInputHandler.invokeOverAllPasses(upAfterMove)
        verify(recognizer.onStop!!, never()).invoke()
    }

    @Test
    fun pointerInputHandler_downConsumedUp_onStopNotCalled() {
        recognizer.pointerInputHandler.invokeOverAllPasses(downConsumed)
        recognizer.pointerInputHandler.invokeOverAllPasses(up)
        verify(recognizer.onStop!!, never()).invoke()
    }

    @Test
    fun pointerInputHandler_downConsumedMoveConsumed_onStopNotCalled() {
        recognizer.pointerInputHandler.invokeOverAllPasses(downConsumed)
        recognizer.pointerInputHandler.invokeOverAllPasses(moveConsumed)
        verify(recognizer.onStop!!, never()).invoke()
    }

    @Test
    fun pointerInputHandler_downDownUp_onStopNotCalled() {
        val down0 = down(0)
        val down1 = down(1)
        val up0 = down0.up()
        recognizer.pointerInputHandler.invokeOverAllPasses(down0)
        recognizer.pointerInputHandler.invokeOverAllPasses(down1)
        recognizer.pointerInputHandler.invokeOverAllPasses(up0)
        verify(recognizer.onStop!!, never()).invoke()
    }

    @Test
    fun pointerInputHandler_downMoveConsumed_onCancelCalledOnce() {
        recognizer.pointerInputHandler.invokeOverAllPasses(down)
        recognizer.pointerInputHandler.invokeOverAllPasses(moveConsumed)
        verify(recognizer.onCancel!!).invoke()
    }

    @Test
    fun pointerInputHandler_downMoveConsumedMoveConsumed_onCancelCalledOnce() {
        val down = down(x = 0f)
        val move1 = down.moveTo(x = 5f)
        val move2 = move1.moveTo(x = 10f)
        recognizer.pointerInputHandler.invokeOverAllPasses(down)
        recognizer.pointerInputHandler.invokeOverAllPasses(move1.consume(dx=1f))
        recognizer.pointerInputHandler.invokeOverAllPasses(move2.consume(dx=1f))
        verify(recognizer.onCancel!!).invoke()
    }

    @Test
    fun pointerInputHandler_downDownMoveConsumedMoveConsumed_onCancelCalledOnce() {
        val down1 = down(x = 0f)
        val down2 = down(x = 100f)
        val move1 = down1.moveTo(x = 5f)
        val move2 = down2.moveTo(x = 105f)
        recognizer.pointerInputHandler.invokeOverAllPasses(down1)
        recognizer.pointerInputHandler.invokeOverAllPasses(down2)
        recognizer.pointerInputHandler.invokeOverAllPasses(move1.consume(dx=1f))
        recognizer.pointerInputHandler.invokeOverAllPasses(move2.consume(dx=1f))
        verify(recognizer.onCancel!!).invoke()
    }

    @Test
    fun pointerInputHandler_downDownMoveMoveConsumed_onCancelCalledOnce() {
        val down1 = down(x = 0f)
        val down2 = down(x = 100f)
        val move1 = down1.moveTo(x = 5f)
        val move2 = down2.moveTo(x = 105f)
        recognizer.pointerInputHandler.invokeOverAllPasses(down1)
        recognizer.pointerInputHandler.invokeOverAllPasses(down2)
        recognizer.pointerInputHandler.invokeOverAllPasses(move1)
        recognizer.pointerInputHandler.invokeOverAllPasses(move2.consume(dx=1f))
        verify(recognizer.onCancel!!).invoke()
    }

    @Test
    fun pointerInputHandler_downConsumedMoveConsumed_onCancelNotCalled() {
        recognizer.pointerInputHandler.invokeOverAllPasses(downConsumed)
        recognizer.pointerInputHandler.invokeOverAllPasses(moveConsumed)
        verify(recognizer.onCancel!!, never()).invoke()
    }

    @Test
    fun pointerInputHandler_downUp_onCancelNotCalled() {
        recognizer.pointerInputHandler.invokeOverAllPasses(down)
        recognizer.pointerInputHandler.invokeOverAllPasses(up)
        verify(recognizer.onCancel!!, never()).invoke()
    }

    @Test
    fun pointerInputHandler_downMoveUp_onCancelNotCalled() {
        recognizer.pointerInputHandler.invokeOverAllPasses(down)
        recognizer.pointerInputHandler.invokeOverAllPasses(move)
        recognizer.pointerInputHandler.invokeOverAllPasses(upAfterMove)
        verify(recognizer.onCancel!!, never()).invoke()
    }

    @Test
    fun pointerInputHandler_down_downPositionIsCorrect() {
        recognizer.pointerInputHandler.invokeOverAllPasses(down(x = 13f, y = 17f))
        verify(recognizer.onStart!!).invoke(Offset(13f, 17f))
    }
}