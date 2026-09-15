package com.mmushtaq.smartreceiptscanner.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.Collections
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.mmushtaq.smartreceiptscanner.R
import com.mmushtaq.smartreceiptscanner.ads.BannerAd
import com.mmushtaq.smartreceiptscanner.core.data.Categories
import com.mmushtaq.smartreceiptscanner.core.data.SpendSummaryBuilder
import com.mmushtaq.smartreceiptscanner.core.data.db.CategoryTotal
import com.mmushtaq.smartreceiptscanner.core.util.formatMinor
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onOpenCamera: () -> Unit,
    onImagePicked: (Uri) -> Unit,
    onPdfPicked: (Uri) -> Unit,
    onOpenHistory: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
    vm: HomeViewModel = koinViewModel()
) {
    val context = LocalContext.current
    val monthlyTotals by vm.monthlyCategoryTotals.collectAsState()
    val baseCurrency by vm.baseCurrency.collectAsState()
    val rates by vm.rates.collectAsState()

    // --- Launchers ---
    val pickPhoto = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri -> uri?.let(onImagePicked) }

    val openDocImage = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            takePersistableIfPossible(context, it)
            onImagePicked(it)
        }
    }

    val openPdf = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            takePersistableIfPossible(context, it)
            onPdfPicked(it)
        }
    }

    // --- Actions model for grid ---
    val actions = listOf(
        HomeAction(stringResource(R.string.camera),
            stringResource(R.string.capture_a_new_receipt), Icons.Outlined.CameraAlt) {
            onOpenCamera()
        },
        HomeAction(stringResource(R.string.gallery),
            stringResource(R.string.import_a_photo_of_a_receipt), Icons.Outlined.Collections) {
            if (ActivityResultContracts.PickVisualMedia.isPhotoPickerAvailable(context)) {
                pickPhoto.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            } else {
                openDocImage.launch(arrayOf("image/*"))
            }
        },
        HomeAction(stringResource(R.string.pdf),
            stringResource(R.string.import_a_pdf_of_a_receipt), Icons.Outlined.Description) {
            openPdf.launch(arrayOf("application/pdf"))
        },
        HomeAction(stringResource(R.string.history),
            stringResource(R.string.view_your_saved_receipts), Icons.Outlined.History) {
            onOpenHistory()
        }
    )

    Scaffold(
        topBar = {
            LargeTopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
                actions = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Outlined.Settings, contentDescription = "Settings")
                    }
                }
            )
        },
        bottomBar = {
            BannerAd(
            )
        },
    ) { pad ->
        Column(
            modifier = modifier
                .padding(pad)
                .fillMaxSize()
        ) {
            // Hero band
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.linearGradient(
                            listOf(
                                MaterialTheme.colorScheme.primaryContainer,
                                MaterialTheme.colorScheme.secondaryContainer
                            )
                        )
                    )
                    .padding(20.dp)
            ) {
                Column {
                    Text(
                        "Get started",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold)
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        stringResource(R.string.scan_a_new_receipt),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            MonthlySummaryCard(monthlyTotals, baseCurrency, rates)

            Spacer(Modifier.height(12.dp))

            // Grid of actions
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 170.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(actions) { action ->
                    FeatureCard(
                        title = action.title,
                        subtitle = action.subtitle,
                        icon = action.icon,
                        onClick = action.onClick
                    )
                }
            }
        }
    }
}

@Composable
private fun FeatureCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    ElevatedCard(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(36.dp))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun MonthlySummaryCard(totals: List<CategoryTotal>, baseCurrency: String, rates: Map<String, Double>) {
    val summary = remember(totals, baseCurrency, rates) {
        SpendSummaryBuilder.build(totals, baseCurrency, rates)
    }
    if (summary.byCurrency.isEmpty()) return

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                "This month",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(4.dp))

            val combinedTotal = summary.combinedTotalMinor
            val combinedCategories = summary.combinedCategoryTotals

            if (combinedTotal != null && combinedCategories != null) {
                Text(combinedTotal.formatMinor(summary.baseCurrency), style = MaterialTheme.typography.headlineSmall)
                if (summary.byCurrency.size > 1) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        "Combined from ${summary.byCurrency.size} currencies",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(Modifier.height(14.dp))

                combinedCategories.forEach { c ->
                    val cat = Categories.byId(c.category)
                    CategoryBar(
                        label = cat.label,
                        color = cat.color,
                        amountText = c.totalMinor.formatMinor(summary.baseCurrency),
                        fraction = if (combinedTotal > 0) c.totalMinor / combinedTotal.toFloat() else 0f
                    )
                    Spacer(Modifier.height(8.dp))
                }
            } else {
                // Can't safely combine yet — show per-currency subtotals instead of misleading bars.
                summary.byCurrency.forEach { group ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(group.currency, style = MaterialTheme.typography.bodyMedium)
                        Text(group.totalMinor.formatMinor(group.currency), style = MaterialTheme.typography.bodyMedium)
                    }
                    Spacer(Modifier.height(6.dp))
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    "Add exchange rates in Settings to see a combined total.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun CategoryBar(label: String, color: Color, amountText: String, fraction: Float) {
    Column {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, style = MaterialTheme.typography.bodyMedium)
            Text(amountText, style = MaterialTheme.typography.bodyMedium)
        }
        Spacer(Modifier.height(4.dp))
        Box(
            Modifier
                .fillMaxWidth()
                .height(6.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(3.dp))
        ) {
            Box(
                Modifier
                    .fillMaxWidth(fraction.coerceIn(0f, 1f))
                    .fillMaxHeight()
                    .background(color, RoundedCornerShape(3.dp))
            )
        }
    }
}

private data class HomeAction(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val onClick: () -> Unit
)

private fun takePersistableIfPossible(context: Context, uri: Uri) {
    try {
        context.contentResolver.takePersistableUriPermission(
            uri,
            Intent.FLAG_GRANT_READ_URI_PERMISSION
        )
    } catch (_: SecurityException) {
        // Some pickers/providers won't allow it; ignore.
    }

}

@Preview
@Composable
fun FeatureCardPreview() {
//    FeatureCard("Camera", "Capture a new receipt", Icons.Outlined.CameraAlt) { }

    HomeScreen(
        onOpenCamera = { /*TODO*/ },
        onImagePicked = { /*TODO*/ },
        onPdfPicked = { /*TODO*/ },
        onOpenHistory = { /*TODO*/ },
        onOpenSettings = { /*TODO*/ }
    )
}