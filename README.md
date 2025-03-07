# AI that talks on its own

Naiyasu is a bot that will behave as if a character is chatting with you and others on Discord. Imagine if you gave a character a phone and they'd chat with you throughout their day and talk about whats going on in their life.

## How it works

Naiyasu is the main bot that handles each character's personalities, random events, channels, etc.

The individual characters handled by Naiyasu use the defined channel and webhook to appear as the chosen character. Their 'typed' messages are sent through a webhook in their channel, and responses to them are 'read' by the Naiyasu bot.

Character details are stored in JSON files that describe their personalities, world details, activity times, random events, name, channel and webhook to communicate in.

Character files are entirely editable. You can add, remove, and modify characters. It just needs to end in .json, have all relevant information correctly formatted, and be in the `ServerFiles/Characters/` directory.

Each described character file runs its own schedule, and will randomly send messages on its own based on the day segment it currently is. The day segments define the chance of the character speaking on their own in 'idle' mode, as well as the chance they will respond to a message in 'chat' mode.
