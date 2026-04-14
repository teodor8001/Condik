package com.example.workipi.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.workipi.data.model.EmployeeLevel

@Composable
fun LevelBadge(level: EmployeeLevel, modifier: Modifier = Modifier) {
    val (bgColor, textColor) = when (level) {
        EmployeeLevel.JUNIOR -> Pair(Color(0xFFE3F2FD), Color(0xFF1565C0)) // albastru
        EmployeeLevel.MID    -> Pair(Color(0xFFFFF8E1), Color(0xFFF57F17)) // galben
        EmployeeLevel.SENIOR -> Pair(Color(0xFFE8F5E9), Color(0xFF2E7D32)) // verde
        EmployeeLevel.LEAD   -> Pair(Color(0xFFFFEBEE), Color(0xFFC62828)) // rosu
    }
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(bgColor)
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(
            text = level.label,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = textColor
        )
    }
}