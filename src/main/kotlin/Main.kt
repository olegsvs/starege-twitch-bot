import com.github.philippheuer.credentialmanager.domain.OAuth2Credential
import com.github.philippheuer.events4j.simple.SimpleEventHandler
import com.github.twitch4j.TwitchClient
import com.github.twitch4j.TwitchClientBuilder
import com.github.twitch4j.chat.events.channel.ChannelMessageEvent
import com.github.twitch4j.common.enums.CommandPermission
import com.github.twitch4j.eventsub.domain.RedemptionStatus
import com.github.twitch4j.helix.TwitchHelix
import com.github.twitch4j.helix.TwitchHelixBuilder
import com.github.twitch4j.helix.domain.AnnouncementColor
import com.github.twitch4j.helix.domain.BanUserInput
import com.github.twitch4j.helix.domain.ChannelInformation
import com.github.twitch4j.helix.domain.CustomReward
import com.github.twitch4j.pubsub.domain.PredictionOutcome
import com.github.twitch4j.pubsub.events.PredictionCreatedEvent
import com.github.twitch4j.pubsub.events.PredictionUpdatedEvent
import com.github.twitch4j.pubsub.events.RewardRedeemedEvent
import io.github.cdimascio.dotenv.Dotenv
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.*
import kotlinx.serialization.json.Json
import java.time.Duration

val dotenv = Dotenv.load()

//  TODO(@olegsvs) fix chars, WTF
val dodoPromo = dotenv.get("DODO_PROMO").replace("'", "")
val staregeBotAccessToken = dotenv.get("SENTRY_OAUTH_TOKEN").replace("'", "")
val twitchChannelAccessToken = dotenv.get("CHANNEL_OAUTH_TOKEN").replace("'", "")
val staregeBotOAuth2Credential = OAuth2Credential("twitch", staregeBotAccessToken)
val twitchChannelOAuth2Credential = OAuth2Credential("twitch", twitchChannelAccessToken)
val youtubeApiKey = dotenv.get("YOUTUBE_API_KEY").replace("'", "")
val youtubeChannelKey = dotenv.get("YOUTUBE_CHANNEL_KEY").replace("'", "")
val broadcasterId = dotenv.get("BROADCASTER_ID").replace("'", "")
val twitchClientId = dotenv.get("TWITCH_CLIENT_ID").replace("'", "")
val twitchClientSecret = dotenv.get("TWITCH_CLIENT_SECRET").replace("'", "")
val moderatorId = dotenv.get("MODERATOR_ID").replace("'", "")
val rewardDodoTitle = "Промокод на ДОДО пиццу за 1р (ТОЛЬКО РФ)"
var rewardDodoID: String? = null
val youtubeCommandTriggers = listOf("!yt", "!ютуб", "!youtube")
val helixClient: TwitchHelix = TwitchHelixBuilder.builder()
    .withClientId(twitchClientId)
    .withClientSecret(twitchClientSecret)
    .withLogLevel(feign.Logger.Level.FULL)
    .build()
val twitchClient: TwitchClient = TwitchClientBuilder.builder()
    .withEnableChat(true)
    .withChatAccount(staregeBotOAuth2Credential)
    .withEnableHelix(true)
    .withEnablePubSub(true)
    .withClientId(twitchClientId)
    .withClientSecret(twitchClientSecret)
    .withFeignLogLevel(feign.Logger.Level.FULL)
    .withDefaultEventHandler(SimpleEventHandler::class.java)
    .build()
val httpClient = HttpClient(CIO) {
    expectSuccess = true
    install(Logging)
    install(ContentNegotiation) {
        json(Json {
            prettyPrint = true
            isLenient = true
            ignoreUnknownKeys = true
        })
    }
}

@OptIn(DelicateCoroutinesApi::class)
fun main(args: Array<String>) {
    twitchClient.chat.joinChannel("c_a_k_e")
    twitchClient.eventManager.onEvent(ChannelMessageEvent::class.java) { event ->
        if (event.message.startsWith("!title ")) {
            changeTitle(event, event.message.removePrefix("!title "))
        }
        if (event.message.startsWith("!sgame ")) {
            changeCategory(event, event.message.removePrefix("!sgame "))
        }
        if (youtubeCommandTriggers.contains(event.message.trim())) {
            GlobalScope.launch {
                getLastYoutubeHighlight(event)
            }
        }
        if (event.message.startsWith("!sping")) {
            pingCommand(event)
        }
        if (event.message.startsWith("!fight")) {
            GlobalScope.launch {
                startDuelCommand(event)
            }

        }
        if (event.message.startsWith("!gof")) {
            GlobalScope.launch {
                assignDuelCommand(event)
            }
        }
    }

    twitchClient.pubSub.connect()

    val cRewards =
        helixClient.getCustomRewards(twitchChannelOAuth2Credential.accessToken, broadcasterId, null, null).execute()
    for (reward in cRewards.rewards) {
//        if (reward.title.equals("test") || reward.title.equals("0aa7d314-6ac2-4806-9527-4530f74ebf19")) {
//            helixClient.deleteCustomReward(
//                twitchChannelOAuth2Credential.accessToken,
//                broadcasterId,
//                reward.id,
//            ).execute()
//        }
        if (reward.title.equals(rewardDodoTitle)) {
            rewardDodoID = reward.id
            val customRewards = helixClient.getCustomRewardRedemption(
                twitchChannelOAuth2Credential.accessToken,
                broadcasterId,
                rewardDodoID,
                null,
                RedemptionStatus.UNFULFILLED,
                null,
                null,
                null
            ).execute()
            val redemptionsToCancelIds = customRewards.redemptions.map { it.redemptionId }
            if (redemptionsToCancelIds.isNotEmpty()) {
                helixClient.updateRedemptionStatus(
                    twitchChannelOAuth2Credential.accessToken,
                    broadcasterId, rewardDodoID, redemptionsToCancelIds, RedemptionStatus.CANCELED
                ).execute()
            }
        }
    }
    if (cRewards.rewards.none { it.title.equals(rewardDodoTitle) }) {
        val customReward = CustomReward()
            .withCost(1500000)
            .withTitle(rewardDodoTitle)
            .withIsUserInputRequired(false)
            .withBackgroundColor("#FF6900")
            .withIsEnabled(true)
            .withMaxPerStreamSetting(
                CustomReward.MaxPerStreamSetting().toBuilder()
                    .isEnabled(true)
                    .maxPerStream(1).build()
            )
            .withMaxPerUserPerStreamSetting(
                CustomReward.MaxPerUserPerStreamSetting().toBuilder()
                    .isEnabled(true)
                    .maxPerUserPerStream(1).build()
            )
        helixClient.createCustomReward(twitchChannelOAuth2Credential.accessToken, broadcasterId, customReward).execute()
    } else {
        val customReward = CustomReward()
            .withCost(1500000)
            .withTitle(rewardDodoTitle)
        helixClient.updateCustomReward(
            twitchChannelOAuth2Credential.accessToken,
            broadcasterId,
            rewardDodoID,
            customReward
        ).execute()
    }

    twitchClient.pubSub.listenForChannelPointsRedemptionEvents(twitchChannelOAuth2Credential, broadcasterId)
    twitchClient.pubSub.listenForChannelPredictionsEvents(twitchChannelOAuth2Credential, broadcasterId)
    twitchClient.eventManager.onEvent(RewardRedeemedEvent::class.java, ::onRewardRedeemed)
    twitchClient.eventManager.onEvent(PredictionCreatedEvent::class.java, ::onPredictionCreatedEvent)
    twitchClient.eventManager.onEvent(PredictionUpdatedEvent::class.java, ::onPredictionUpdatedEvent)
}

private fun onRewardRedeemed(rewardRedeemedEvent: RewardRedeemedEvent) {
    try {
        if (rewardRedeemedEvent.redemption.reward.id.equals(rewardDodoID)) {
            val user = rewardRedeemedEvent.redemption.user
            helixClient.sendWhisper(
                staregeBotOAuth2Credential.accessToken,
                moderatorId,
                user.id,
                "Ваш промокод на додо-пиццу за 1р: $dodoPromo"
            ).execute()
            twitchClient.chat.sendMessage(
                "c_a_k_e",
                "peepoFat \uD83C\uDF55  @${user.displayName} отправил вам промокод в ЛС :) Если он не пришёл, то возможно у вас заблокирована личка для незнакомых, напишите в личку @Sentry__Ward"
            )
            helixClient.updateRedemptionStatus(
                twitchChannelOAuth2Credential.accessToken,
                broadcasterId, rewardDodoID, listOf(rewardRedeemedEvent.redemption.id), RedemptionStatus.FULFILLED
            ).execute()
        }
    } catch (e: Throwable) {
        println("Failed onRewardRedeemed: $e")
        helixClient.updateRedemptionStatus(
            twitchChannelOAuth2Credential.accessToken,
            broadcasterId, rewardDodoID, listOf(rewardRedeemedEvent.redemption.id), RedemptionStatus.CANCELED
        ).execute()
        twitchClient.chat.sendMessage(
            "c_a_k_e",
            "@${rewardRedeemedEvent.redemption.user.displayName}, ошибка отправки промо додо-пиццы, возможно у вас отключено принятие сообщений в ЛС в настройках приватности, попробуйте позднее Sadge"
        )
    }
}

private fun onPredictionCreatedEvent(predictionCreatedEvent: PredictionCreatedEvent) {
    helixClient.sendChatAnnouncement(
        staregeBotOAuth2Credential.accessToken,
        broadcasterId,
        moderatorId,
        "PepegaPhone СТАВКА",
        AnnouncementColor.PURPLE
    ).execute()
}

private fun onPredictionUpdatedEvent(predictionUpdatedEvent: PredictionUpdatedEvent) {
    if (predictionUpdatedEvent.event.status.equals("RESOLVED")) {
        val win: PredictionOutcome =
            predictionUpdatedEvent.event.outcomes.first { it.id.equals(predictionUpdatedEvent.event.winningOutcomeId) }
        when (win.color.name) {
            "BLUE" -> helixClient.sendChatAnnouncement(
                staregeBotOAuth2Credential.accessToken,
                broadcasterId,
                moderatorId,
                "BlueWin",
                AnnouncementColor.PURPLE
            ).execute()

            "PINK" -> helixClient.sendChatAnnouncement(
                staregeBotOAuth2Credential.accessToken,
                broadcasterId,
                moderatorId,
                "RedWin",
                AnnouncementColor.PURPLE
            ).execute()
        }
    }
}


private fun changeTitle(event: ChannelMessageEvent, newTitle: String) {
    try {
        println(event.permissions)
        if (event.permissions.contains(CommandPermission.MODERATOR) || event.permissions.contains(CommandPermission.BROADCASTER)) {
            if (newTitle.isEmpty()) {
                event.reply(twitchClient.chat, "DinkDonk Укажите текст")
                return
            }
            helixClient.updateChannelInformation(
                twitchChannelOAuth2Credential.accessToken,
                broadcasterId,
                ChannelInformation().withTitle(newTitle)
            ).execute()
            event.reply(twitchClient.chat, "Название изменено на $newTitle")
        }
    } catch (e: Throwable) {
        println("Failed changeTitle: $e")
    }
}

private fun changeCategory(event: ChannelMessageEvent, newCategory: String) {
    try {
        if (event.permissions.contains(CommandPermission.MODERATOR) || event.permissions.contains(CommandPermission.BROADCASTER)) {
            if (newCategory.isEmpty()) {
                event.reply(twitchClient.chat, "DinkDonk Укажите название категории")
                return
            }
            val response =
                helixClient.getGames(staregeBotOAuth2Credential.accessToken, listOf(), listOf(newCategory), listOf())
                    .execute()
            if (response.games.isEmpty()) {
                event.reply(twitchClient.chat, "Категория $newCategory не найдена Sadge")

            } else {
                println(response.games)
                val game = response.games[0]
                helixClient.updateChannelInformation(
                    twitchChannelOAuth2Credential.accessToken,
                    broadcasterId,
                    ChannelInformation().withGameId(game.id)
                ).execute()
                event.reply(twitchClient.chat, "Категория изменена на ${game.name}")
            }
        }
    } catch (e: Throwable) {
        println("Failed changeCategory: $e")
    }
}

private fun pingCommand(event: ChannelMessageEvent) {
    println("pingCommand")
    try {
        event.reply(
            twitchClient.chat,
            "Starege pong, доступные команды: ${getEnabledCommands()}. For PETTHEMODS : !title, !sgame - change title/category"
        )
    } catch (e: Throwable) {
        println("Failed pingCommand: $e")
    }
}

private fun getEnabledCommands(): String {
    return "!sping, !fight, !yt"
}

var lastYoutubeCommand: Long? = null
private suspend fun getLastYoutubeHighlight(event: ChannelMessageEvent) {
    println("getLastYoutubeHighlight")
    try {
        if (lastYoutubeCommand != null) {
            val diff = System.currentTimeMillis() - lastYoutubeCommand!!
            if (diff < 3000) return
        }
        lastYoutubeCommand = System.currentTimeMillis()
        val playlists: YoutubeChannelResponse =
            httpClient.request("https://www.googleapis.com/youtube/v3/channels?id=${youtubeChannelKey}&key=${youtubeApiKey}&part=contentDetails")
                .body()
        val playListID = playlists.items[0].contentDetails.relatedPlaylists.uploads
        println("youtube playListID: ${playListID}")
        val videosInPlayList: YoutubeVideosResponse =
            httpClient.request("https://www.googleapis.com/youtube/v3/playlistItems?playlistId=${playListID}&key=${youtubeApiKey}&part=snippet&maxResults=20")
                .body()
        var found: VideoItem? = null
        for (video in videosInPlayList.items) {
            if (video.snippet.description.contains("shorts") or video.snippet.title.contains("@CakeStream"))
                continue
            found = video
        }
        if (found != null) {
            val title = found.snippet.title
            val videoUrl = "https://www.youtube.com/watch?v=${found.snippet.resourceId.videoId}"
            val finalText =
                "Подписывайся на ютуб - https://goo.su/W5UDBz ! Ежедневно новые шортсы и очень часто новые хайлайты. Последний хайлайт - $title $videoUrl"
            event.reply(twitchClient.chat, finalText)
        } else {
            println("getLastYoutubeHighlight: video not found")
            event.reply(
                twitchClient.chat,
                "Подписывайся на ютуб - https://goo.su/W5UDBz ! Ежедневно новые шортсы и очень часто новые хайлайты."
            )
        }

    } catch (e: Throwable) {
        println("Failed getLastYoutubeHighlight: $e")
        event.reply(
            twitchClient.chat,
            "Подписывайся на ютуб - https://goo.su/W5UDBz ! Ежедневно новые шортсы и очень часто новые хайлайты."
        )
    }
}

var duelIsStarted = false
var duelFirstUserMessage: ChannelMessageEvent? = null
var duelSecondUserMessage: ChannelMessageEvent? = null
var lastDuel: Long? = System.currentTimeMillis()
private suspend fun startDuelCommand(event: ChannelMessageEvent) {
    println("duel request")
    try {
        if (event.permissions.contains(CommandPermission.MODERATOR) || event.permissions.contains(CommandPermission.BROADCASTER)) {
            event.reply(
                twitchClient.chat,
                "Вы неуязвимы для боя EZ"
            )
            return
        }

        if (duelIsStarted) {
            assignDuelCommand(event)
            println("Duel: another duel already started")
            return
        }

        val now = System.currentTimeMillis() / 1000
        if (lastDuel != null) {
            val diff = (now - lastDuel!! / 1000)
            if (diff < 300) {
                val nextRollTime = (300 - diff)
                val nextRollMinutes = (nextRollTime % 3600) / 60
                val nextRollSeconds = (nextRollTime % 3600) % 60
                event.reply(
                    twitchClient.chat,
                    "Ринг отмывают, осталось \uD83D\uDD5B ${nextRollMinutes}m${nextRollSeconds}s Modge"
                )
                println("Duel: diff")
                return
            }
        }
        duelIsStarted = true
        duelFirstUserMessage = event
        event.reply(
            twitchClient.chat,
            "@${event.user.name} ищет смельчака на бой, проигравшему - мут на 10 минут, напиши !gof, чтобы принять вызов(ожидание - 1 минута)"
        )
        println("Duel: wait another user")
        delay(Duration.ofMinutes(1).toMillis())
        println("Duel: clean duel 1")
        duelFirstUserMessage = null
        duelSecondUserMessage = null
        duelIsStarted = false
    } catch (e: Throwable) {
        println("Duel: clean duel 2")
        duelFirstUserMessage = null
        duelSecondUserMessage = null
        duelIsStarted = false
        println("Failed fightCommand: $e")
    }
}

private suspend fun assignDuelCommand(event: ChannelMessageEvent) {
    println("duel assign request")
    try {
        if (event.permissions.contains(CommandPermission.MODERATOR) || event.permissions.contains(CommandPermission.BROADCASTER)) {
            event.reply(
                twitchClient.chat,
                "Вы неуязвимы для боя EZ"
            )
            return
        }
        if (!duelIsStarted) {
            return
        }

        if (duelFirstUserMessage == null) {
            duelFirstUserMessage = null
            duelSecondUserMessage = null
            duelIsStarted = false
            return
        }

        if (!duelFirstUserMessage!!.user.id.equals(event.user.id)) {
            duelSecondUserMessage = event
        }
        val now = System.currentTimeMillis()
        lastDuel = now
        duelIsStarted = false
        /*
        await chat_bot.send_message(TARGET_CHANNEL, 'Opachki начинается дуэль между ' + duel_first_user_message.user.name + ' и ' + duel_second_user_message.user.name)
        await asyncio.sleep(5)
         */
        event.reply(
            twitchClient.chat,
            "Opachki начинается дуэль между ${duelFirstUserMessage!!.user.name} и ${duelSecondUserMessage!!.user.name}"
        )
        delay(5000)
        val rnd = (0..1).random()
        val winner: ChannelMessageEvent?
        val looser: ChannelMessageEvent?
        if (rnd == 0) {
            winner = duelFirstUserMessage!!
            looser = duelSecondUserMessage!!
        } else {
            winner = duelSecondUserMessage!!
            looser = duelFirstUserMessage!!
        }
        event.reply(
            twitchClient.chat,
            "${winner.user.name}  EZ победил в поединке против forsenLaughingAtYou ${looser.user.name} и отправил его отдыхать на 10 минут SadgeCry"
        )
        println("Duel: clean duel 6")
        duelFirstUserMessage = null
        duelSecondUserMessage = null
        duelIsStarted = false
        delay(5000)
        helixClient.banUser(
            staregeBotOAuth2Credential.accessToken,
            broadcasterId,
            moderatorId,
            BanUserInput()
                .withUserId(looser.user.id)
                .withDuration(600)
                .withReason("duel with ${winner.user.name}")
        )
    } catch (e: Throwable) {
        println("Duel: clean duel 7")
        duelFirstUserMessage = null
        duelSecondUserMessage = null
        duelIsStarted = false
        println("Failed assignDuel: $e")
    }
}

private fun askGPT() {
    // TODO
}