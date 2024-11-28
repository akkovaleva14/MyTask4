package com.example.task4

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.RectF
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.core.view.children
import kotlin.math.pow
import kotlin.math.sqrt

@SuppressLint("ClickableViewAccessibility")
class EmojiButtonManager(
    private val context: Context,
    private val mainLayout: FrameLayout,
    private val buttonSize: Int,
    private val buttonSpacing: Int
) {
    private val emojiButtons: List<Button>
    private var isExpanded = false
    private var viewToMove: ImageView? = null

    private val ringPaint = Paint().apply {
        color = context.resources.getColor(R.color.colorAccent)
        style = Paint.Style.STROKE
        strokeWidth = 8f
    }
    private val linePaint = Paint().apply {
        color = context.resources.getColor(R.color.colorPrimary)
        style = Paint.Style.STROKE
        strokeWidth = 4f
        pathEffect = android.graphics.DashPathEffect(floatArrayOf(10f, 5f), 0f)
    }
    private val path = Path()
    private var firstFinger: Pair<Float, Float>? = null
    private var secondFinger: Pair<Float, Float>? = null
    private var topChildView: View? = null
    private var initialDistance: Float = 0f
    private var initialScaleX: Float = 1f
    private var initialScaleY: Float = 1f

    init {
        emojiButtons = List(5) { index ->
            Button(context).apply {
                setBackgroundResource(
                    when (index) {
                        0 -> R.drawable.ic_green
                        1 -> R.drawable.ic_grin
                        2 -> R.drawable.ic_smile
                        3 -> R.drawable.ic_tongue
                        4 -> R.drawable.ic_blue
                        else -> 0
                    }
                )
                layoutParams = FrameLayout.LayoutParams(buttonSize, buttonSize).apply {
                    gravity = android.view.Gravity.BOTTOM or android.view.Gravity.END
                    setMargins(16, 16, 16, 16)
                }
                visibility = View.GONE
                setOnClickListener {
                    addEmojiToCenter(index)
                }
            }.also { button ->
                mainLayout.addView(button)
            }
        }

        val drawingView = DrawingView(context)
        mainLayout.addView(drawingView)

        mainLayout.setOnTouchListener { _, event ->
            handleTouch(event, drawingView)
            true
        }

        addClearButton()
    }

    private fun addClearButton() {
        val clearButton = Button(context).apply {
            setBackgroundResource(R.drawable.ic_cry)
            layoutParams = FrameLayout.LayoutParams(buttonSize, buttonSize).apply {
                gravity = android.view.Gravity.BOTTOM or android.view.Gravity.START
                setMargins(16, 16, 0, 16)
            }
            setOnClickListener {
                clearAllEmojis()
            }
        }
        mainLayout.addView(clearButton)
    }

    private fun clearAllEmojis() {
        for (i in mainLayout.childCount - 1 downTo 0) {
            val child = mainLayout.getChildAt(i)
            if (child is ImageView) {
                mainLayout.removeView(child)
            }
        }
        viewToMove = null
    }

    private var firstFingerDownX = 0f
    private var firstFingerDownY = 0f

    @SuppressLint("ClickableViewAccessibility")
    private fun handleTouch(event: MotionEvent, drawingView: DrawingView) {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                // Первый палец касается экрана.
                firstFingerDownX = event.x
                firstFingerDownY = event.y
                firstFinger = Pair(event.x, event.y)
                updateCurrentEmojiImageView(event.x, event.y)
                Log.d("HandleTouch", "ACTION_DOWN: x=${event.x}, y=${event.y}")
            }

            MotionEvent.ACTION_POINTER_DOWN -> {
                // Второй палец касается экрана
                if (event.pointerCount == 2) {
                    firstFinger = Pair(event.getX(0), event.getY(0))
                    secondFinger = Pair(event.getX(1), event.getY(1))

                    topChildView = getTopChildView()

                    if (topChildView != null && isViewBetweenFingers(topChildView)) {
                        // Обновляем порядок наложения для верхней вью
//                        topChildView?.z = 6f  // Устанавливаем значение z для этой вью

                        // Вычисляем начальную дистанцию между пальцами
                        initialDistance = calculateDistance(firstFinger!!, secondFinger!!)
                        // Сохраняем начальные значения масштаба
                        initialScaleX = topChildView?.scaleX ?: 1f
                        initialScaleY = topChildView?.scaleY ?: 1f
                    }
                    Log.d(
                        "HandleTouch",
                        "ACTION_POINTER_DOWN: firstFinger=$firstFinger, secondFinger=$secondFinger"
                    )
                    Log.d(
                        "HandleTouch",
                        "Top view detected: ${topChildView != null}, initialDistance=$initialDistance"
                    )
                }
                drawingView.invalidate()
            }

            MotionEvent.ACTION_MOVE -> {
                if (event.pointerCount == 1) {
                    firstFinger = Pair(event.x, event.y)
//                    viewToMove?.let {
//                        it.translationX = event.x - firstFingerDownX
//                        it.translationY = event.y - firstFingerDownY
//                    }
                    viewToMove?.let {
                        // Получаем текущие координаты viewToMove
                        val currentX = it.x
                        val currentY = it.y

                        // Обновляем координаты с учетом начальных значений
                        it.x = currentX + (event.x - firstFingerDownX)
                        it.y = currentY + (event.y - firstFingerDownY)

                        // Обновляем начальные координаты для следующего движения
                        firstFingerDownX = event.x
                        firstFingerDownY = event.y
                    }
                    Log.d("HandleTouch", "ACTION_MOVE (single touch): x=${event.x}, y=${event.y}")

                } else if (event.pointerCount == 2) {
                    firstFinger = Pair(event.getX(0), event.getY(0))
                    secondFinger = Pair(event.getX(1), event.getY(1))
                    topChildView = getTopChildView()

                    // Если два пальца на экране и вью между ними, обновляем масштаб
                    if (topChildView != null && isViewBetweenFingers(topChildView)) {
                        val distance = calculateDistance(firstFinger!!, secondFinger!!)
                        if (initialDistance > 0f) {
                            adjustTopViewScale(distance)
                        }
                        Log.d(
                            "HandleTouch",
                            "ACTION_MOVE (multi-touch): firstFinger=$firstFinger, secondFinger=$secondFinger, distance=$distance"
                        )
                    }
                }
                drawingView.invalidate()
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP, MotionEvent.ACTION_CANCEL -> {
                // Окончание взаимодействия с пальцами
                if (event.pointerCount == 1) {
                    firstFinger = null
                    initialDistance = 0f
                    Log.d(
                        "HandleTouch",
                        "ACTION_UP or ACTION_CANCEL (single touch): x=${event.x}, y=${event.y}"
                    )
                } else if (event.pointerCount == 2) {
                    secondFinger = null
                    Log.d(
                        "HandleTouch",
                        "ACTION_POINTER_UP or ACTION_CANCEL (multi-touch): second finger lifted"
                    )
                }
                firstFingerDownX = 0f
                firstFingerDownY = 0f
                viewToMove = null
                drawingView.invalidate()
            }
        }
    }

    private fun adjustTopViewScale(currentDistance: Float) {
        topChildView?.let { view ->
            // Вычисление коэффициента масштаба
            val scaleFactor = currentDistance / initialDistance
            val newScaleX = (initialScaleX * scaleFactor).coerceIn(0.5f, 6f)
            val newScaleY = (initialScaleY * scaleFactor).coerceIn(0.5f, 6f)

            // Применение масштаба к вью
            view.scaleX = newScaleX
            view.scaleY = newScaleY
        }
    }

    private fun isViewBetweenFingers(view: View?): Boolean {
        if (view == null || firstFinger == null || secondFinger == null) return false

        val rect = RectF(view.x, view.y, view.x + view.width, view.y + view.height)
        val lineStart = firstFinger!!
        val lineEnd = secondFinger!!

        return rect.intersects(
            minOf(lineStart.first, lineEnd.first),
            minOf(lineStart.second, lineEnd.second),
            maxOf(lineStart.first, lineEnd.first),
            maxOf(lineStart.second, lineEnd.second)
        )
    }

    private fun updateCurrentEmojiImageView(x: Float, y: Float) {
        mainLayout.children.forEach { child ->
            if (child is ImageView && child != viewToMove) {
                Log.d(
                    "EmojiButtonManager",
                    "Child bounds: x=${child.x}, y=${child.y}, width=${child.width}, height=${child.height}"
                )
                Log.d("EmojiButtonManager", "Touch point: x=$x, y=$y")

                if (x >= child.x && x <= child.x + child.width &&
                    y >= child.y && y <= child.y + child.height
                ) {
                    // Устанавливаем выбранную иконку как верхнюю
                    child.bringToFront()
                    viewToMove = child
                    // Обновляем отображение, чтобы изменения вступили в силу
                    mainLayout.invalidate()
                    return@forEach
                }
            }
        }
    }

    private fun getTopChildView(): View? {
        var topView: View? = null
        for (i in 0 until mainLayout.childCount) {
            val child = mainLayout.getChildAt(i)
            if (topView == null || child.z > topView.z) {
                topView = child
            }
        }
        return topView
    }

    private fun calculateDistance(point1: Pair<Float, Float>, point2: Pair<Float, Float>): Float {
        return sqrt((point2.first - point1.first).pow(2) + (point2.second - point1.second).pow(2))
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun addEmojiToCenter(index: Int) {
        val emojiResId = when (index) {
            0 -> R.drawable.ic_green
            1 -> R.drawable.ic_grin
            2 -> R.drawable.ic_smile
            3 -> R.drawable.ic_tongue
            4 -> R.drawable.ic_blue
            else -> return
        }

        val newEmojiView = ImageView(context).apply {
            setImageResource(emojiResId)
            layoutParams = FrameLayout.LayoutParams(160, 160).apply {
                gravity = android.view.Gravity.CENTER
            }
            setBackgroundColor(context.resources.getColor(R.color.transparent))

//            initialScaleX = scaleX
//            initialScaleY = scaleY
//            initialDistance = 0f

//            setOnTouchListener { v, event ->
//                when (event.action) {
//                    MotionEvent.ACTION_DOWN -> {
//                        v.bringToFront()
//                        v.z = 6f
//                        resetOtherViewsZ(v)
//                        true
//                    }
//
//                    MotionEvent.ACTION_MOVE -> {
//                        if (event.pointerCount == 2) {
//                            firstFinger = Pair(event.getX(0), event.getY(0))
//                            secondFinger = Pair(event.getX(1), event.getY(1))
//
//                            val distance = calculateDistance(firstFinger!!, secondFinger!!)
//                            adjustTopViewScale(distance)
//                        }
//                        v.x = event.x - v.width / 2
//                        v.y = event.y - v.height / 2
//                        true
//                    }
//
//                    else -> false
//                }
//            }
        }
        mainLayout.addView(newEmojiView)
    }

    fun toggleEmojiButtons() {
        isExpanded = !isExpanded
        if (isExpanded) {
            showEmojiButtons()
        } else {
            hideEmojiButtons()
        }
    }

    private fun showEmojiButtons() {
        emojiButtons.forEachIndexed { index, button ->
            button.visibility = View.VISIBLE
            button.translationY = 0f
            button.alpha = 0f
            button.animate()
                .translationY((-(index + 1) * (buttonSize + buttonSpacing)).toFloat())
                .alpha(1f)
                .setDuration(300)
                .setStartDelay(index * 50L)
                .start()
        }
    }

    private fun hideEmojiButtons() {
        emojiButtons.forEachIndexed { index, button ->
            button.animate()
                .translationY(0f)
                .alpha(0f)
                .setDuration(300)
                .setStartDelay(index * 50L)
                .withEndAction {
                    button.visibility = View.GONE
                }
                .start()
        }
    }

    inner class DrawingView(context: Context) : View(context) {
        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)

            firstFinger?.let { finger1 ->
                canvas.drawCircle(finger1.first, finger1.second, 50f, ringPaint)
                Log.d(
                    "EmojiButtonManager",
                    "onDraw: Drawing circle for 1st finger at (${finger1.first}, ${finger1.second})"
                )
            }

            secondFinger?.let { finger2 ->
                canvas.drawCircle(finger2.first, finger2.second, 50f, ringPaint)
                Log.d(
                    "EmojiButtonManager",
                    "onDraw: Drawing circle for 2d finger at (${finger2.first}, ${finger2.second})"
                )

                path.reset()
                path.moveTo(firstFinger!!.first, firstFinger!!.second)
                path.lineTo(finger2.first, finger2.second)
                canvas.drawPath(path, linePaint)
                Log.d(
                    "EmojiButtonManager",
                    "onDraw: Drawing line between 1st finger (${firstFinger!!.first}, ${firstFinger!!.second}) and 2d finger (${finger2.first}, ${finger2.second})"
                )
            }
        }
    }
}

