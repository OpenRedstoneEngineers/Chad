package org.openredstone.managers

import org.javacord.api.DiscordApi
import org.openredstone.commands.Command
import org.openredstone.commands.ErrorCommand
import org.openredstone.commands.StaticCommand
import org.openredstone.model.context.CommandContext
import org.openredstone.model.entity.CommandEntity
import org.pircbotx.PircBotX

import java.util.ArrayList
import java.util.Arrays
import java.util.Optional

class CommandManager(private val discordApi: DiscordApi, private val commandChar: Char) {
    private val commands: MutableList<Command> = ArrayList()

    fun listenOnDiscord() {
        discordApi.addMessageCreateListener { event ->
            if (!event.messageAuthor.asUser().get().isBot) {
                getAttemptedCommand(CommandContext.DISCORD, event.messageContent).ifPresent { command ->
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
        Thread {
            ircBot.startBot()
        }.start()
    }

    fun addCommand(command: Command): CommandManager {
        commands.add(command)
        return this
    }

    fun addStaticCommands(commandEntities: List<CommandEntity>) : CommandManager {
        commandEntities.forEach { commands.add(StaticCommand(it.context, it.name, it.reply)) }
        return this
    }

    fun getAttemptedCommand(commandContext: CommandContext, message: String): Optional<Command> {
        if (message.isEmpty() || message[0] != commandChar) {
            return Optional.empty()
        }

        val args = message.split(" ")

        var executedCommand = commands.stream()
                .filter { command ->
                    command.command == parseCommandName(args.toTypedArray())
                            && (command.type == CommandContext.BOTH || command.type == commandContext)
                }.findFirst()

        if (!executedCommand.isPresent) {
            executedCommand = Optional.of(ErrorCommand())
        }

        if (args.toTypedArray().size - 1 < executedCommand.get().requireParameters) {
            executedCommand.get().reply = "Invalid number of arguments passed to command `" + executedCommand.get().command + "`"
        } else {
            executedCommand.get().runCommand(dropFirstItem(args.toTypedArray()))
        }

        return executedCommand
    }

    private fun parseCommandName(array: Array<String>): String {
        return array[0].substring(1)
    }

    private fun dropFirstItem(array: Array<String>): Array<String> {
        return if (array.size < 2) {
            arrayOf()
        } else {
            Arrays.copyOfRange(array, 1, array.size)
        }
    }
}