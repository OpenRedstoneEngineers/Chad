package org.openredstone.managers

import kotlin.concurrent.thread

import org.javacord.api.DiscordApi
import org.openredstone.commands.Command
import org.openredstone.commands.ErrorCommand
import org.openredstone.commands.StaticCommand
import org.openredstone.commands.CommandContext
import org.openredstone.model.entity.CommandEntity
import org.pircbotx.PircBotX

class CommandManager(private val discordApi: DiscordApi, private val commandChar: Char) {
    private val commands = mutableListOf<Command>()

    fun listenOnDiscord() {
        discordApi.addMessageCreateListener { event ->
            if (!event.messageAuthor.asUser().get().isBot) {
                getAttemptedCommand(CommandContext.DISCORD, event.messageContent)?.let { command ->
                    if (command.isPrivateMessageResponse) {
                        event.messageAuthor.asUser().ifPresent { user ->
                            user.sendMessage(command.reply)
                        }
                    } else {
                        event.channel.sendMessage(command.reply)
                    }
                }
            }
        }
    }

    fun listenOnIrc(ircBot: PircBotX) {
        thread {
            ircBot.startBot()
        }
    }

    fun addCommands(vararg commandsToAdd: Command) {
        commands.addAll(commandsToAdd)
    }

    fun addStaticCommands(commandEntities: List<CommandEntity>) {
        commandEntities.forEach { commands.add(StaticCommand(it.context, it.name, it.reply)) }
    }

    fun getAttemptedCommand(commandContext: CommandContext, message: String): Command? {
        if (message.isEmpty() || message[0] != commandChar) {
            return null
        }

        val args = message.split(" ")

        val executedCommand = commands.asSequence()
            .filter { command ->
                command.command == parseCommandName(args)
                        && (command.type == CommandContext.BOTH || command.type == commandContext)
            }.firstOrNull() ?: ErrorCommand()

        if (args.size - 1 < executedCommand.requireParameters) {
            executedCommand.reply = "Invalid number of arguments passed to command `" + executedCommand.command + "`"
        } else {
            executedCommand.runCommand(args.drop(1))
        }
        return executedCommand
    }

    private fun parseCommandName(parts: List<String>) = parts[0].substring(1)
}
