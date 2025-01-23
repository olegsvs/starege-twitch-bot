import java.sql.Connection
import java.sql.DriverManager
import java.sql.PreparedStatement

data class Users(
    val users: MutableList<User>,
    val MySQL_HOST: String,
    val MySQL_PORT: String,
    val MySQL_PASSWORD: String,
    val MySQL_USER: String,
) {
    fun getByIdOrCreate(userId: String, userName: String): User {
        var user: User? = users.firstOrNull { it.userId == userId }
        if (user == null) {
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
            val query =
                "INSERT INTO duel_users (user_id, duel_wins, duel_loses, user_name) VALUES (?, ?, ?, ?) ON DUPLICATE KEY UPDATE duel_wins=VALUES(duel_wins), duel_loses=VALUES(duel_loses)"
            val conn2 =
                DriverManager.getConnection("jdbc:mysql://${MySQL_HOST}:${MySQL_PORT}/twitch_starege_bot?user=${MySQL_USER}&password=${MySQL_PASSWORD}");
            val stmt3: PreparedStatement = conn2.prepareStatement(query)
            stmt3.setString(1, updatedUser.userId)
            stmt3.setInt(2, updatedUser.duelWins)
            stmt3.setInt(3, updatedUser.duelLoses)
            stmt3.setString(4, updatedUser.userName)
            stmt3.executeUpdate()
            stmt3.closeOnCompletion()
            return true
        } else {
            return false
        }
    }

    fun getMaxWinsUser(): User {
        return users.maxBy { it.duelWins }
    }

    fun getMaxLosesUser(): User {
        return users.maxBy { it.duelLoses }
    }

    companion object {
        fun init(
            MySQL_HOST: String,
            MySQL_PORT: String,
            MySQL_PASSWORD: String,
            MySQL_USER: String,
        ): Users {
            val conn =
                DriverManager.getConnection("jdbc:mysql://${MySQL_HOST}:${MySQL_PORT}/twitch_starege_bot?user=${MySQL_USER}&password=${MySQL_PASSWORD}");
            val stmtDuels = conn.createStatement();
            val rsDuels = stmtDuels.executeQuery("SELECT * from duel_users");
            val usersLocal: MutableList<User> = mutableListOf()
            while (rsDuels.next()) {
                usersLocal.add(
                    User(
                        userId = rsDuels.getString(2),
                        userName = rsDuels.getString(5),
                        duelWins = rsDuels.getInt(3),
                        duelLoses = rsDuels.getInt(4),
                    )
                )
            }
            return Users(
                usersLocal,
                MySQL_HOST,
                MySQL_PORT,
                MySQL_PASSWORD,
                MySQL_USER
            );
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

    fun win(toAdd: Int = 1): User {
        duelWins += toAdd
        return this
    }
}