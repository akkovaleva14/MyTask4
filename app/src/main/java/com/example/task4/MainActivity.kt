package com.example.task4

import android.os.Bundle
import android.widget.Button
import android.widget.FrameLayout
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.task4.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var customButton: Button
    private lateinit var emojiButtonManager: EmojiButtonManager

    private val buttonSize = 100 // Фиксированный размер кнопок
    private val buttonSpacing = 16 // Фиксированное расстояние между кнопками

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater) // Инициализация View Binding
        setContentView(binding.root) // Устанавливаем корень из View Binding

        // Инициализация основного layout
        val mainLayout = binding.main

        // Создаем кастомную кнопку
        customButton = Button(this).apply {
            setBackgroundResource(R.drawable.ic_devil)
            layoutParams = FrameLayout.LayoutParams(
                buttonSize,
                buttonSize
            ).apply {
                gravity = android.view.Gravity.BOTTOM or android.view.Gravity.END
                setMargins(16, 16, 16, 16) // Устанавливаем отступы от краев экрана
            }
        }

        // Добавляем стартовую кнопку в основной layout
        mainLayout.addView(customButton)

        // Инициализация EmojiButtonManager
        emojiButtonManager = EmojiButtonManager(this, mainLayout, buttonSize, buttonSpacing)

        // Устанавливаем обработчик клика для кастомной кнопки
        customButton.setOnClickListener {
            emojiButtonManager.toggleEmojiButtons()
        }

        ViewCompat.setOnApplyWindowInsetsListener(mainLayout) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }
}
