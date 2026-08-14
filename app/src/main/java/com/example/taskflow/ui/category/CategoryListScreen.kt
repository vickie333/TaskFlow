package com.example.taskflow.ui.category

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun CategoryScreen(viewModel: CategoryViewModel, onBack: () -> Unit) {
    val categories by viewModel.categories.collectAsState()
    val msg by viewModel.msg.collectAsState()
    var newCategoryName by remember() { mutableStateOf("") }

    Column(modifier = Modifier
        .fillMaxSize()
        .padding(vertical = 16.dp),
        verticalArrangement = Arrangement.Center) {
        Button(onClick = onBack,
            modifier = Modifier.padding(16.dp)) {
            Text("Volver")
        }
        Row(modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp))
        {
            OutlinedTextField(newCategoryName, onValueChange = {
                    userText -> newCategoryName = userText
            }, modifier = Modifier.weight(1f))
            Spacer(modifier = Modifier.width(16.dp))
            Button(onClick = {
                viewModel.addCategory(newCategoryName)
                newCategoryName = ""
            }) {
                Text("Añade una categoría")
            }
        }

        if (msg != null) {
            Text("$msg", modifier = Modifier.padding(horizontal = 16.dp))
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            items(categories) { category ->
                Row(modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Categoría: ${category.name}",
                        fontSize = 20.sp,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Button(onClick = { viewModel.removeCategory(category)}) {
                        Text(text = "Eliminar categoria")
                    }
                }
            }
        }
    }
}