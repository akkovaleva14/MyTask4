package com.example.task4

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
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
    private var currentEmojiImageView: ImageView? = null

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
        Log.d("EmojiButtonManager", "init")
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
        currentEmojiImageView = null
    }

    private fun handleTouch(event: MotionEvent, drawingView: DrawingView) {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                firstFinger = Pair(event.x, event.y)
                updateCurrentEmojiImageView(event.x, event.y)
                drawingView.invalidate()
            }

            MotionEvent.ACTION_MOVE -> {
                if (event.pointerCount == 1) {
                    firstFinger = Pair(event.x, event.y)
                } else if (event.pointerCount == 2) {
                    firstFinger = Pair(event.getX(0), event.getY(0))
                    secondFinger = Pair(event.getX(1), event.getY(1))

                    topChildView = getTopChildView()

                    if (isViewBetweenFingers(topChildView)) {
                        val distance = calculateDistance(firstFinger!!, secondFinger!!)
                        if (initialDistance == 0f) {
                            initialDistance = distance
                            // Store the initial scale of the top view
                            initialScaleX = topChildView?.scaleX ?: 1f
                            initialScaleY = topChildView?.scaleY ?: 1f
                        } else {
                            adjustTopViewScale(distance)
                        }
                    }
                }
                updateCurrentEmojiImageView(event.x, event.y)
                drawingView.invalidate()
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP -> {
                if (event.pointerCount == 1) {
                    firstFinger = null
                    initialDistance = 0f
                } else if (event.pointerCount == 2) {
                    secondFinger = null
                }
                drawingView.invalidate()
            }
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

    private fun adjustTopViewScale(currentDistance: Float) {
        topChildView?.let { view ->
            val scaleFactor = currentDistance / initialDistance
            val newScaleX = (initialScaleX * scaleFactor).coerceIn(0.5f, 6f)
            val newScaleY = (initialScaleY * scaleFactor).coerceIn(0.5f, 6f)

            view.scaleX = newScaleX
            view.scaleY = newScaleY
        }
    }

    private fun updateCurrentEmojiImageView(x: Float, y: Float) {
        for (i in 0 until mainLayout.childCount) {
            val child = mainLayout.getChildAt(i)
            if (child is ImageView && child != currentEmojiImageView) {
                if (x >= child.x && x <= child.x + child.width && y >= child.y && y <= child.y + child.height) {
                    if (child.tag != "ic_devil") {
                        currentEmojiImageView = child
                    }
                    break
                }
            }
        }
    }

    private fun getTopChildView(): View? {
        var topView: View? = null
        var maxZIndex = -Float.MAX_VALUE // Start with a very low value to find the top view

        for (i in 0 until mainLayout.childCount) {
            val child = mainLayout.getChildAt(i)
            // Compare the actual z-index of each child
            if (child.z > maxZIndex) {
                topView = child
                maxZIndex = child.z
            }
        }

        Log.d("EmojiButtonManager", "Top child view: ${topView?.tag}")
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

        val emojiTag = when (index) {
            0 -> "ic_green"
            1 -> "ic_grin"
            2 -> "ic_smile"
            3 -> "ic_tongue"
            4 -> "ic_blue"
            else -> "unknown" // на случай, если индекс будет выходить за пределы
        }

        currentEmojiImageView = ImageView(context).apply {
            setImageResource(emojiResId)
            layoutParams = FrameLayout.LayoutParams(160, 160).apply {
                gravity = android.view.Gravity.CENTER
            }
            setBackgroundColor(context.resources.getColor(R.color.transparent))
            tag = emojiTag

            // Bring the emoji to front as soon as it's created
            bringToFront() // Move it to the front before adding to the layout
            z = 6f // Ensure it's on top in terms of z-index

            // Log the view being brought to the front
            Log.d("EmojiButtonManager", "Bringing to front1: ${this.tag}")

            setOnTouchListener { v, event ->
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        v.bringToFront()
                        v.z = 6f
                        Log.d("EmojiButtonManager", "Bringing to front2: ${v.tag}")
                        v.requestLayout()
                        v.invalidate()
                        v.post {
                            updateTopChildView() // Отложить обновление до завершения UI операций
                        }
                        Log.d("EmojiButtonManager", "Bringing to front3: ${v.tag}")
                        true
                    }

                    MotionEvent.ACTION_MOVE -> {
                        v.x = event.rawX - (v.width / 2)
                        v.y = event.rawY - (v.height / 2)
                        true
                    }

                    MotionEvent.ACTION_UP -> {
                        true
                    }

                    else -> false
                }
            }
        }

        mainLayout.addView(currentEmojiImageView)
    }

    private fun updateTopChildView() {
        // Ensure topChildView is updated after any changes
        topChildView = getTopChildView()
        Log.d("EmojiButtonManager", "Updated top child view: ${topChildView?.tag}")
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
            }

            secondFinger?.let { finger2 ->
                canvas.drawCircle(finger2.first, finger2.second, 50f, ringPaint)

                path.reset()
                path.moveTo(firstFinger!!.first, firstFinger!!.second)
                path.lineTo(finger2.first, finger2.second)
                canvas.drawPath(path, linePaint)
            }
        }
    }
}