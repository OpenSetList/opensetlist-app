package com.opensetlist.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

enum class DrawerSection {
    ALL_SONGS, SETLISTS, ARTISTS, TAGS, SETTINGS
}

@Composable
fun SideDrawer(
    currentSection: DrawerSection,
    onSectionSelected: (DrawerSection) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxHeight()
            .width(280.dp)
            .background(MaterialTheme.colorScheme.surface)
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(modifier = Modifier.height(48.dp))

        Text(
            text = "Setlist App",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(32.dp))

        DrawerItem(
            label = "Todas as Músicas",
            isSelected = currentSection == DrawerSection.ALL_SONGS,
            onClick = { onSectionSelected(DrawerSection.ALL_SONGS) }
        )

        Spacer(modifier = Modifier.height(8.dp))

        DrawerItem(
            label = "Set Lists",
            isSelected = currentSection == DrawerSection.SETLISTS,
            onClick = { onSectionSelected(DrawerSection.SETLISTS) }
        )

        Spacer(modifier = Modifier.height(8.dp))

        DrawerItem(
            label = "Artistas",
            isSelected = currentSection == DrawerSection.ARTISTS,
            onClick = { onSectionSelected(DrawerSection.ARTISTS) }
        )

        Spacer(modifier = Modifier.height(8.dp))

        DrawerItem(
            label = "Tags",
            isSelected = currentSection == DrawerSection.TAGS,
            onClick = { onSectionSelected(DrawerSection.TAGS) }
        )

        Spacer(modifier = Modifier.height(8.dp))

        DrawerItem(
            label = "Configurações",
            isSelected = currentSection == DrawerSection.SETTINGS,
            onClick = { onSectionSelected(DrawerSection.SETTINGS) }
        )
    }
}

@Composable
private fun DrawerItem(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .background(
                if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                else Color.Transparent
            )
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = if (isSelected) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurface
        )
    }
}
