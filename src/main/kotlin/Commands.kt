data class Commands(
    val commands: MutableList<Command>
) {
    fun isEnabled(name: String): Boolean {
        val command = commands.firstOrNull { it.name == name }
        if(command != null) {
            return command.enabled
        } else {
            return false
        }
    }

    fun setEnabled(name: String, value: Boolean): Boolean {
        val command = commands.firstOrNull { it.name == name }
        if(command != null) {
            commands.remove(command)
            commands.add(Command(name = command.name, enabled = value))
            return true
        } else {
            return false
        }
    }
}

data class Command(
    val name: String,
    val enabled: Boolean
) {}