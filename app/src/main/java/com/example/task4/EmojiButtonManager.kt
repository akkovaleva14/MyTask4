package com.example.task4

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
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
    private var currentEmojiImageView: ImageView? = null // Ссылка на текущее изображение эмодзи

    // Для рисования кольца и линии
    private val ringPaint = Paint().apply {
        color = context.resources.getColor(R.color.colorAccent)
        style = Paint.Style.STROKE
        strokeWidth = 8f
    }
    private val linePaint = Paint().apply {
        color = context.resources.getColor(R.color.colorPrimary)
        style = Paint.Style.STROKE
        strokeWidth = 4f
        pathEffect = android.graphics.DashPathEffect(floatArrayOf(10f, 5f), 0f) // Пунктирная линия
    }
    private val path = Path()
    private var firstFinger: Pair<Float, Float>? = null
    private var secondFinger: Pair<Float, Float>? = null
    private var topChildView: View? = null // Ссылка на верхний элемент
    private var initialDistance: Float = 0f // Начальное расстояние между пальцами

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
                layoutParams = FrameLayout.LayoutParams(
                    buttonSize,
                    buttonSize
                ).apply {
                    gravity = android.view.Gravity.BOTTOM or android.view.Gravity.END
                    setMargins(16, 16, 16, 16) // Устанавливаем отступы для кнопок эмодзи
                }
                visibility = View.GONE

                // Устанавливаем обработчик клика для добавления эмодзи
                setOnClickListener {
                    addEmojiToCenter(index)
                }
            }.also { button ->
                // Добавляем каждую кнопку в основной layout
                mainLayout.addView(button)
            }
        }

        // Добавляем кастомный View для рисования
        val drawingView = DrawingView(context)
        mainLayout.addView(drawingView)

        // Устанавливаем обработчик касаний на основной layout
        mainLayout.setOnTouchListener { _, event ->
            handleTouch(event, drawingView)
            true
        }

        // Добавляем кнопку для очистки вью
        addClearButton()
    }

    private fun addClearButton() {
        val clearButton = Button(context).apply {
            setBackgroundResource(R.drawable.ic_cry)
            layoutParams = FrameLayout.LayoutParams(
                buttonSize,
                buttonSize
            ).apply {
                gravity = android.view.Gravity.BOTTOM or android.view.Gravity.START
                setMargins(16, 16, 0, 16) // Устанавливаем отступы для кнопки очистки
            }
            // Устанавливаем обработчик клика для очистки
            setOnClickListener {
                clearAllEmojis()
            }
        }

        // Добавляем кнопку очистки в основной layout
        mainLayout.addView(clearButton)
    }

    private fun clearAllEmojis() {
        // Удаляем все ImageView, которые были добавлены
        for (i in mainLayout.childCount - 1 downTo 0) {
            val child = mainLayout.getChildAt(i)
            if (child is ImageView) {
                mainLayout.removeView(child)
            }
        }
        currentEmojiImageView = null // Сбрасываем ссылку на текущее изображение эмодзи
    }

    private fun handleTouch(event: MotionEvent, drawingView: DrawingView) {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                firstFinger = Pair(event.x, event.y)
                drawingView.invalidate() // Перерисовываем
            }

            MotionEvent.ACTION_MOVE -> {
                if (event.pointerCount == 1) {
                    firstFinger = Pair(event.x, event.y)
                } else if (event.pointerCount == 2) {
                    firstFinger = Pair(event.getX(0), event.getY(0))
                    secondFinger = Pair(event.getX(1), event.getY(1))

                    // Сохраняем ссылку на верхний childView
                    topChildView = getTopChildView()

                    // Вычисляем расстояние между пальцами
                    val distance = calculateDistance(firstFinger!!, secondFinger!!)
                    if (initialDistance == 0f) {
                        initialDistance = distance // Сохраняем начальное расстояние
                    } else {
                        adjustTopViewScale(distance)
                    }
                }
                drawingView.invalidate() // Перерисовываем
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP -> {
                if (event.pointerCount == 1) {
                    firstFinger = null
                    initialDistance = 0f // Сброс начального расстояния
                } else if (event.pointerCount == 2) {
                    secondFinger = null
                }
                drawingView.invalidate() // Перерисовываем
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

    private fun adjustTopViewScale(currentDistance: Float) {
        topChildView?.let { view ->
            // Вычисляем новый масштаб на основе изменения расстояния между пальцами
            val scaleFactor = currentDistance / initialDistance
            val newScaleX = (view.scaleX * scaleFactor).coerceIn(0.5f, 6f)
            val newScaleY = (view.scaleY * scaleFactor).coerceIn(0.5f, 6f)

            // Применяем новый масштаб к верхнему элементу
            view.scaleX = newScaleX
            view.scaleY = newScaleY
        }
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

        // Создаем ImageView для эмодзи
        currentEmojiImageView = ImageView(context).apply {
            setImageResource(emojiResId)
            layoutParams = FrameLayout.LayoutParams(160, 160).apply {
                gravity = android.view.Gravity.CENTER // Центрируем изображение
            }
            setBackgroundColor(context.resources.getColor(R.color.transparent)) // Устанавливаем прозрачный фон

            // Устанавливаем OnTouchListener для перетаскивания
            setOnTouchListener { v, event ->
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        // Поднимаем вью над другими элементами
                        v.bringToFront()
                        v.z = 6f // Устанавливаем Z-позицию
                        v.requestLayout() // Обновляем расположение вью
                        v.invalidate() // Обновляем отображение
                        true
                    }

                    MotionEvent.ACTION_MOVE -> {
                        // Двигаем вью вслед за пальцем
                        v.x = event.rawX - (v.width / 2)
                        v.y = event.rawY - (v.height / 2)
                        true
                    }

                    MotionEvent.ACTION_UP -> {
                        // Можно добавить логику при отпускании пальца, если нужно
                        true
                    }

                    else -> false
                }
            }
        }

        // Добавляем ImageView в центр mainLayout
        mainLayout.addView(currentEmojiImageView)
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
            button.translationY = 0f // Начальная позиция под кнопкой
            button.alpha = 0f // Начальная прозрачность
            button.animate()
                .translationY((-(index + 1) * (buttonSize + buttonSpacing)).toFloat()) // Фиксированное расстояние между кнопками
                .alpha(1f)
                .setDuration(300)
                .setStartDelay(index * 50L) // Задержка для эффекта "выезда"
                .start()
        }
    }

    private fun hideEmojiButtons() {
        emojiButtons.forEachIndexed { index, button ->
            button.animate()
                .translationY(0f) // Возвращаем на место
                .alpha(0f)
                .setDuration(300)
                .setStartDelay(index * 50L) // Задержка для эффекта "опускания"
                .withEndAction {
                    button.visibility = View.GONE // Скрываем кнопку после анимации
                }
                .start()
        }
    }

    // Класс для кастомного View, который будет рисовать кольца и линии
    inner class DrawingView(context: Context) : View(context) {
        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)

            firstFinger?.let { finger1 ->
                // Рисуем кольцо вокруг первого пальца
                canvas.drawCircle(finger1.first, finger1.second, 50f, ringPaint)
            }

            secondFinger?.let { finger2 ->
                // Рисуем кольцо вокруг второго пальца
                canvas.drawCircle(finger2.first, finger2.second, 50f, ringPaint)

                // Рисуем линию между пальцами
                path.reset()
                path.moveTo(firstFinger!!.first, firstFinger!!.second)
                path.lineTo(finger2.first, finger2.second)
                canvas.drawPath(path, linePaint)
            }
        }
    }
}
