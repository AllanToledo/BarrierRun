package com.allantoledo.barrierrun

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.allantoledo.barrierrun.ui.theme.BarrierRunTheme


class MainActivity : ComponentActivity() {

    var boxSize: Float = 0.0f

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val displayMetrics = applicationContext.resources.displayMetrics
        val width = displayMetrics.widthPixels / displayMetrics.density
        boxSize = (width - (18 * 3)) / 9
        setContent {
            BarrierRunTheme {
                Surface(color = MaterialTheme.colors.background) {
                    Body("Android")
                }
            }
        }
    }


    @Composable
    fun Body(name: String) {
        var playerOneTurn by remember { mutableStateOf(false) }
        var verticalBuilding by remember { mutableStateOf(false) }
        var horizontalBuilding by remember { mutableStateOf(false) }
        var removingBarrier by remember { mutableStateOf(false) }
        val playerOne by remember { mutableStateOf(GamePlayer(4, 0, Color.Blue)) }
        val playerTwo by remember { mutableStateOf(GamePlayer(4, 8, Color.Red)) }
        val barriers by remember { mutableStateOf(ArrayList<Barrier>()) }
        val displayMetrics = LocalContext.current.resources.displayMetrics
        val width = displayMetrics.widthPixels / displayMetrics.density
        
        Column(
            Modifier
                .fillMaxSize()
                .background(Color.DarkGray),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {

            ConstructionBarrierMode(
                enabled = playerOneTurn && playerOne.barriersRemaining > 0,
                barrierRemaining = playerOne.barriersRemaining,
                onSelectVertical = {
                    horizontalBuilding = false
                    removingBarrier = false
                    verticalBuilding = !verticalBuilding
                },
                onSelectRemove = {
                    verticalBuilding = false
                    horizontalBuilding = false
                    removingBarrier = !removingBarrier
                },
                onSelectHorizontal = {
                    verticalBuilding = false
                    removingBarrier = false
                    horizontalBuilding = !horizontalBuilding
                }
            )

            Box() {
                val playerFocused = if (playerOneTurn) playerOne else playerTwo
                val opponent = if (playerOneTurn) playerTwo else playerOne
                LazyColumn() {
                    gridItems(
                        rowCount = 9,
                        columnCount = 9,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier
                    ) { rowIndex, columnIndex ->
                        var playerAbove: GamePlayer? = null
                        if (playerOne.x == columnIndex && playerOne.y == rowIndex)
                            playerAbove = playerOne
                        if (playerTwo.x == columnIndex && playerTwo.y == rowIndex)
                            playerAbove = playerTwo
                        TableCell(
                            isAvailable = getPositionViability(
                                playerFocused,
                                opponent,
                                barriers,
                                rowIndex,
                                columnIndex
                            ) && !horizontalBuilding && !verticalBuilding,
                            onSelect = {
                                playerFocused.x = columnIndex
                                playerFocused.y = rowIndex
                                playerOneTurn = !playerOneTurn
                            },
                            playerAbove = playerAbove
                        )
                    }
                }
                Box(
                    Modifier
                        .height(width.dp)
                        .width(width.dp)
                ) {
                    barriers.forEach { barrier ->
                        BarrierCell(
                            barrier,
                            removing = removingBarrier,
                            onSelect = {
                                playerFocused.barriersRemaining -= 1
                                barriers.remove(barrier)
                                playerOneTurn = !playerOneTurn
                            }
                        )
                    }
                }
                if (horizontalBuilding)
                    Box(
                        Modifier
                            .height(width.dp)
                            .width(width.dp)
                    ) {
                        for (columnIndex in 0..7) {
                            for (rowIndex in 1..8) {
                                if (horizontalBuildingAvailable(barriers, columnIndex, rowIndex))
                                    Box(
                                        Modifier.offset(
                                            ((columnIndex * (boxSize + 6)) - 12).dp,
                                            ((rowIndex * (boxSize + 6)) - 12).dp
                                        )
                                    ) {
                                        BuildingCell(
                                            onSelect = {
                                                barriers.add(Barrier(columnIndex, rowIndex, false))
                                                horizontalBuilding = false
                                                playerFocused.barriersRemaining -= 1
                                                playerOneTurn = !playerOneTurn
                                            },
                                            vertical = false
                                        )
                                    }
                            }
                        }
                    }
                if (verticalBuilding)
                    Box(
                        Modifier
                            .height(width.dp)
                            .width(width.dp)
                    ) {
                        for (columnIndex in 1..8) {
                            for (rowIndex in 0..7) {
                                if (verticalBuildingAvailable(barriers, columnIndex, rowIndex))
                                    Box(
                                        Modifier.offset(
                                            ((columnIndex * (boxSize + 6)) - 12).dp,
                                            ((rowIndex * (boxSize + 6)) - 12).dp
                                        )
                                    ) {
                                        BuildingCell(
                                            onSelect = {
                                                barriers.add(Barrier(columnIndex, rowIndex, true))
                                                verticalBuilding = false
                                                playerFocused.barriersRemaining -= 1
                                                playerOneTurn = !playerOneTurn
                                            },
                                            vertical = true
                                        )
                                    }
                            }
                        }
                    }

            }
            ConstructionBarrierMode(
                enabled = !playerOneTurn && playerTwo.barriersRemaining > 0,
                barrierRemaining = playerTwo.barriersRemaining,
                onSelectVertical = {
                    horizontalBuilding = false
                    removingBarrier = false
                    verticalBuilding = !verticalBuilding
                },
                onSelectRemove = {
                    verticalBuilding = false
                    horizontalBuilding = false
                    removingBarrier = !removingBarrier
                },
                onSelectHorizontal = {
                    verticalBuilding = false
                    removingBarrier = false
                    horizontalBuilding = !horizontalBuilding
                }
            )

        }
    }

    private fun verticalBuildingAvailable(
        barriers: java.util.ArrayList<Barrier>,
        columnIndex: Int,
        rowIndex: Int
    ): Boolean {
        for (barrier in barriers) {
            if (barrier.x == columnIndex && barrier.y + 1 == rowIndex && barrier.vertical)
                return false
            if (barrier.x == columnIndex && barrier.y == rowIndex && barrier.vertical)
                return false
            if (barrier.x == columnIndex && barrier.y - 1 == rowIndex && barrier.vertical)
                return false
            if (barrier.x + 1 == columnIndex && barrier.y - 1 == rowIndex && !barrier.vertical)
                return false
        }
        return true
    }

    private fun horizontalBuildingAvailable(
        barriers: java.util.ArrayList<Barrier>,
        columnIndex: Int,
        rowIndex: Int
    ): Boolean {
        for (barrier in barriers) {
            if (barrier.x + 1 == columnIndex && barrier.y == rowIndex && !barrier.vertical)
                return false
            if (barrier.x == columnIndex && barrier.y == rowIndex && !barrier.vertical)
                return false
            if (barrier.x - 1 == columnIndex && barrier.y == rowIndex && !barrier.vertical)
                return false
            if (barrier.x - 1 == columnIndex && barrier.y + 1 == rowIndex && barrier.vertical)
                return false
        }
        return true
    }

    @Composable
    fun BuildingCell(
        onSelect: () -> Unit,
        vertical: Boolean
    ) {
        Box(Modifier.alpha(0.4f)) {
            Box(
                Modifier
                    .size(24.dp)
                    .background(
                        if (vertical) Color.Magenta else Color.Green,
                        RoundedCornerShape(boxSize.dp)
                    )
                    .clickable(onClick = onSelect)
            )
        }
    }

    @Composable
    fun ConstructionBarrierMode(
        enabled: Boolean,
        onSelectVertical: () -> Unit,
        onSelectHorizontal: () -> Unit,
        onSelectRemove: () -> Unit,
        barrierRemaining: Int,
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(vertical = 20.dp),
        ) {
            Box(
                Modifier
                    .padding(horizontal = 8.dp)
                    .weight(2f), Alignment.Center
            ) {
                Button(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onSelectHorizontal,
                    enabled = enabled,
                    colors = ButtonDefaults.buttonColors(backgroundColor = Color.Green)
                ) {
                    Text("Horizontal")
                }
            }
            Box(
                Modifier
                    .padding(horizontal = 8.dp)
                    .weight(1f), Alignment.Center
            ) {
                Button(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onSelectRemove,
                    enabled = enabled,
                    colors = ButtonDefaults.buttonColors(backgroundColor = Color.Red)
                ) {
                    Text("$barrierRemaining")
                }
            }
            Box(
                Modifier
                    .padding(horizontal = 8.dp)
                    .weight(2f), Alignment.Center
            ) {
                Button(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onSelectVertical,
                    enabled = enabled,
                    colors = ButtonDefaults.buttonColors(backgroundColor = Color.Magenta)
                ) {
                    Text("Vertical")
                }
            }
        }
    }

    fun getPositionViability(
        player: GamePlayer,
        opponent: GamePlayer,
        barriers: List<Barrier>,
        rowIndex: Int,
        columnIndex: Int
    ): Boolean {
        if (opponent.x == columnIndex && opponent.y == rowIndex)
            return false
        if (player.x + 1 == columnIndex && player.y == rowIndex) {
            for (barrier in barriers) {
                if (barrier.vertical && player.x + 1 == barrier.x && (player.y == barrier.y || player.y - 1 == barrier.y))
                    return false
            }
            return true
        }
        if (player.x - 1 == columnIndex && player.y == rowIndex) {
            for (barrier in barriers) {
                if (barrier.vertical && player.x == barrier.x && (player.y == barrier.y || player.y - 1 == barrier.y))
                    return false
            }
            return true
        }
        if (player.x == columnIndex && player.y + 1 == rowIndex) {
            for (barrier in barriers) {
                if (!barrier.vertical && (player.x == barrier.x || player.x - 1 == barrier.x) && player.y + 1 == barrier.y)
                    return false
            }
            return true
        }
        if (player.x == columnIndex && player.y - 1 == rowIndex) {
            for (barrier in barriers) {
                if (!barrier.vertical && (player.x == barrier.x || player.x - 1 == barrier.x) && player.y == barrier.y)
                    return false
            }
            return true
        }
        return false
    }

    @Composable
    fun BarrierCell(
        barrier: Barrier,
        removing: Boolean,
        onSelect: () -> Unit
    ) {

        Box(
            Modifier
                .offset(
                    ((barrier.x * (boxSize + 6)) - 3).dp,
                    ((barrier.y * (boxSize + 6)) - 3).dp
                )
                .clickable(removing, onClick = onSelect)
        ) {
            if (barrier.vertical)
                Box(
                    Modifier
                        .width(6.dp)
                        .height(((boxSize * 2) + 18).dp)
                        .background(
                            if (removing) Color.Red else Color.Yellow,
                            RoundedCornerShape(3.dp)
                        )
                )
            else
                Box(
                    Modifier
                        .height(6.dp)
                        .width(((boxSize * 2) + 18).dp)
                        .background(
                            if (removing) Color.Red else Color.Yellow,
                            RoundedCornerShape(3.dp)
                        )
                )
        }
    }

    @Composable
    fun TableCell(
        isAvailable: Boolean = false,
        onSelect: () -> Unit,
        playerAbove: GamePlayer?
    ) {
        Box(
            modifier = Modifier
                .padding(3.dp)
                .background(Color.Transparent)
                .clickable(isAvailable, onClick = onSelect)
        ) {
            Surface(elevation = 4.dp, color = Color.Transparent) {
                Box(
                    modifier = Modifier
                        .size(boxSize.dp)
                        .background(colorResource(R.color.black), RoundedCornerShape(2.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    if (isAvailable) {
                        Box(Modifier.alpha(0.2f)) {
                            Box(
                                modifier = Modifier
                                    .size((boxSize * 0.8).dp)
                                    .background(Color.White, RoundedCornerShape(boxSize.dp))
                            )
                        }
                        Box(Modifier.alpha(0.5f)) {
                            Box(
                                modifier = Modifier
                                    .size((boxSize * 0.4).dp)
                                    .background(Color.Cyan, RoundedCornerShape(boxSize.dp))
                            )
                        }
                    }
                    if (playerAbove != null)
                        Box(
                            modifier = Modifier
                                .size((boxSize * 0.9).dp)
                                .background(playerAbove.color, RoundedCornerShape(boxSize.dp))
                        )
                }
            }
        }
    }

    fun LazyListScope.gridItems(
        rowCount: Int,
        columnCount: Int,
        modifier: Modifier,
        horizontalArrangement: Arrangement.Horizontal = Arrangement.Start,
        itemContent: @Composable RowScope.(Int, Int) -> Unit,
    ) {
        items(rowCount, key = { it.hashCode() }) { rowIndex ->
            Row(
                horizontalArrangement = horizontalArrangement,
                modifier = modifier
            ) {
                for (columnIndex in 0 until columnCount) {
                    itemContent(rowIndex, columnIndex)
                }
            }
        }
    }

    @Preview(showBackground = true)
    @Composable
    fun DefaultPreview() {
        BarrierRunTheme {
            Body("Android")
        }
    }
}
