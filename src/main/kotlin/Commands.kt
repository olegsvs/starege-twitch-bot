import com.google.gson.Gson
import com.google.gson.GsonBuilder
import java.io.File

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

    fun save() {
        File("commands.json").writeText(gsonPretty.toJson(this))
    }

    companion object {
        private val gsonPretty: Gson = GsonBuilder().setPrettyPrinting().create()
        fun init(): Commands {
            return gsonPretty.fromJson(File("commands.json").readText(), Commands::class.java) as Commands
        }
    }
}

data class Command(
    val name: String,
    val enabled: Boolean
) {}