package com.example.task4

import android.content.Context
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView

class EmojiButtonManager(
    private val context: Context,
    private val mainLayout: FrameLayout,
    private val buttonSize: Int,
    private val buttonSpacing: Int
) {
    private val emojiButtons: List<Button>
    private var isExpanded = false
    private var currentEmojiImageView: ImageView? = null // Ссылка на текущее изображение эмодзи

    init {
        emojiButtons = List(5) { index ->
            Button(context).apply {
                setBackgroundResource(when (index) {
                    0 -> R.drawable.ic_green
                    1 -> R.drawable.ic_grin
                    2 -> R.drawable.ic_smile
                    3 -> R.drawable.ic_tongue
                    4 -> R.drawable.ic_blue
                    else -> 0
                })
                layoutParams = FrameLayout.LayoutParams(
                    buttonSize,
                    buttonSize
                ).apply {
                    gravity = android.view.Gravity.BOTTOM or android.view.Gravity.END
                    setMargins(8, 8, 8, 8) // Устанавливаем отступы для кнопок эмодзи
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
    }

    private fun addEmojiToCenter(index: Int) {
        // Сначала удаляем предыдущее изображение, если оно существует
        currentEmojiImageView?.let {
            mainLayout.removeView(it)
        }

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
            layoutParams = FrameLayout.LayoutParams(60, 60).apply {
                gravity = android.view.Gravity.CENTER // Центрируем изображение
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
}
