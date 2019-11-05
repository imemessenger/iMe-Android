package org.telegram.ui

import java.util.*

class DialogsTab(var type: DialogsActivity.Tab,
                       var isEnabled: Boolean,
                       var position: Int) : Comparable<DialogsTab> {

    override fun compareTo(o: DialogsTab): Int {
        return Integer.compare(position, o.position)
    }

    override fun equals(o: Any?): Boolean {
        if (this === o) return true
        if (o == null || javaClass != o.javaClass) return false
        val tabState = o as DialogsTab?
        return isEnabled == tabState!!.isEnabled &&
                position == tabState.position &&
                type == tabState.type
    }

    override fun hashCode(): Int {
        return Objects.hash(type, isEnabled, position)
    }

    override fun toString(): String {
        return "TabState{" +
                "type=" + type +
                ", enabled=" + isEnabled +
                ", position=" + position +
                '}'.toString()
    }
}