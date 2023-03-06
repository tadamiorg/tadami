package com.sf.animescraper.ui.utils

import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet

interface ConstraintInstructions
data class ConnectConstraint(val startID: Int, val startSide: Int, val endID: Int, val endSide: Int) : ConstraintInstructions
data class DisconnectConstraint(val startID: Int, val startSide: Int) : ConstraintInstructions


fun ConstraintLayout.updateConstraints(instructions: List<ConstraintInstructions>) {
    ConstraintSet().also {
        it.clone(this)
        for (instruction in instructions) {
            if (instruction is ConnectConstraint) it.connect(instruction.startID, instruction.startSide, instruction.endID, instruction.endSide)
            if (instruction is DisconnectConstraint) it.clear(instruction.startID, instruction.startSide)
        }
        it.applyTo(this)
    }
}