import com.google.gson.Gson
import com.google.gson.GsonBuilder
import java.io.File

data class Users(
    val users: MutableList<User>
) {
    fun getByIdOrCreate(userId: String, userName: String): User {
        var user: User? = users.firstOrNull { it.userId == userId }
        if(user == null) {
            user = User(userId = userId, 0, 0, userName = userName)
            users.add(user)
        }
        return user
    }

    fun update(updatedUser: User): Boolean {
        val user = users.firstOrNull { it.userId == updatedUser.userId }
        if (user != null) {
            users.remove(user)
            users.add(updatedUser)
            return true
        } else {
            return false
        }
    }

    fun save() {
        File("users.json").writeText(gsonPretty.toJson(this))
    }

    fun getMaxWinsUser(): User {
        return users.maxBy { it.duelWins }
    }

    fun getMaxLosesUser(): User {
        return users.maxBy { it.duelLoses }
    }

    companion object {
        private val gsonPretty: Gson = GsonBuilder().setPrettyPrinting().create()
        fun init(): Users {
            return gsonPretty.fromJson(File("users.json").readText(), Users::class.java) as Users
        }
    }
}

data class User(
    val userId: String,
    var duelWins: Int,
    var duelLoses: Int,
    val userName: String,
    ) {
    val stats: String
        get() = "[W$duelWins:L$duelLoses]"

    fun lose(): User {
        duelLoses++
        return this
    }

    fun win(): User {
        duelWins++
        return this
    }
}