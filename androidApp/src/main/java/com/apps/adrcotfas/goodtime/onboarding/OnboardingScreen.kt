/**
 *     Goodtime Productivity
 *     Copyright (C) 2025 Adrian Cotfas
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.apps.adrcotfas.goodtime.onboarding

import android.annotation.SuppressLint
import androidx.activity.compose.BackHandler
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.apps.adrcotfas.goodtime.common.isPortrait
import com.apps.adrcotfas.goodtime.shared.R
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import com.apps.adrcotfas.goodtime.R as AndroidR

val lightGray = Color(0xFFDEDEDE)
val darkGray = Color(0xFF4C4546)

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun OnboardingScreen(viewModel: MainViewModel = koinViewModel()) {
    val pages = OnboardingPage.pages
    val pagerState = rememberPagerState(pageCount = { pages.size })
    val coroutineScope = rememberCoroutineScope()

    BackHandler(pagerState.currentPage != 0) {
        coroutineScope.launch {
            pagerState.animateScrollToPage(pagerState.currentPage - 1)
        }
    }

    Scaffold {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFFAFAFA))
                .systemBarsPadding(),

        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                HorizontalPager(
                    state = pagerState,
                ) { page ->
                    OnboardingPage(
                        title = stringResource(pages[page].title),
                        description1 = stringResource(pages[page].description1),
                        description2 = stringResource(pages[page].description2),
                        image = {
                            Image(
                                painter = painterResource(id = pages[page].image),
                                contentDescription = stringResource(pages[page].title),
                            )
                        },
                    )
                }
            }

            val isLastPage = pagerState.currentPage == pages.lastIndex

            FloatingActionButton(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(32.dp)
                    .size(72.dp),
                containerColor = lightGray,
                contentColor = darkGray,
                elevation = FloatingActionButtonDefaults.bottomAppBarFabElevation(),
                shape = CircleShape,
                onClick = {
                    if (isLastPage) {
                        viewModel.setShowOnboarding(false)
                    } else {
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(pagerState.currentPage + 1)
                        }
                    }
                },
            ) {
                Crossfade(isLastPage, label = "onboarding button") {
                    if (it) {
                        Icon(Icons.Filled.Check, contentDescription = "Finish")
                    } else {
                        Icon(
                            Icons.AutoMirrored.Default.ArrowForward,
                            contentDescription = "Finish",
                        )
                    }
                }
            }

            PageIndicator(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(32.dp),
                pageCount = pages.size,
                currentPage = pagerState.currentPage,
                color = lightGray,
                selectionColor = darkGray,
            )

            IconButton(modifier = Modifier.align(Alignment.TopEnd).padding(end = 16.dp), onClick = {
                viewModel.setShowOnboarding(false)
            }) {
                Icon(
                    imageVector = Icons.Outlined.Close,
                    contentDescription = null,
                    tint = darkGray,
                )
            }
        }
    }
}

@Composable
fun OnboardingPage(
    title: String,
    description1: String,
    description2: String,
    image: @Composable () -> Unit,
) {
    val isPortrait = LocalConfiguration.current.isPortrait
    if (isPortrait) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            image()
            Spacer(modifier = Modifier.padding(16.dp))
            OnboardingPageTextSection(title, description1, description2)
        }
    } else {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 64.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            image()
            Spacer(modifier = Modifier.padding(32.dp))
            OnboardingPageTextSection(title, description1, description2)
        }
    }
}

@Composable
fun OnboardingPageTextSection(title: String, description1: String, description2: String) {
    Column(
        modifier = Modifier.widthIn(max = 400.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        val textColor = Color(0xFF333333)
        Text(text = title, style = MaterialTheme.typography.titleLarge, color = textColor)
        Spacer(modifier = Modifier.padding(8.dp))
        Text(
            text = description1,
            style = MaterialTheme.typography.bodyMedium.copy(textAlign = TextAlign.Center),
            color = textColor,
        )
        Spacer(modifier = Modifier.padding(4.dp))
        Text(
            text = description2,
            style = MaterialTheme.typography.bodyMedium.copy(textAlign = TextAlign.Center),
            color = textColor,
        )
    }
}

@Composable
fun PageIndicator(
    color: Color,
    selectionColor: Color,
    modifier: Modifier = Modifier,
    pageCount: Int,
    currentPage: Int,
) {
    Row(
        modifier
            .height(18.dp)
            .widthIn(max = 128.dp),
        horizontalArrangement = Arrangement.Center,
    ) {
        repeat(pageCount) { iteration ->
            Box(
                modifier = Modifier
                    .padding(4.dp)
                    .clip(CircleShape)
                    .background(if (currentPage == iteration) selectionColor else color)
                    .size(10.dp),

            )
        }
    }
}

@Preview
@Composable
fun OnboardingPagePreview() {
    OnboardingPage(
        title = stringResource(R.string.intro1_title),
        description1 = stringResource(R.string.intro1_desc1),
        description2 = stringResource(R.string.intro1_desc2),
        image = {
            Image(
                painter = painterResource(id = AndroidR.drawable.intro1),
                contentDescription = stringResource(R.string.intro1_title),
            )
        },
    )
}
