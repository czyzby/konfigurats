# Konfigurats: The First Configuration

This game has it all: spaghetti code, unreadable fonts, terrible UI, clunky animations,
security issues, and questionable hitboxes! Relive the worst parts of the golden era
of Flash games, expect this time with Java and without the rose-tinted nostalgia glasses.

Konfigurats is a multiplayer PvP game roughly based on the _Warlock_ mod from _Warcraft III_,
which hilariously I've never played. What I did play was the _Warlocks Arena 2_ Flash game
based on the mod. You can still likely find it online and run it via Ruffle to check out
the story mode, but its multiplayer servers are most certainly offline.

This is my first (semi-)finished project that I've worked on as I was learning programming
and Java in general. While it does have some documentation, don't go looking for tests or
clean code. Let's just say that instead of the SOLID principles, this particular project
followed the YOLO ideology on every step.

The game was never released as such, but maybe it's for the best. After roughly 10 years
of sitting on my hard drive, I've decided to release this project as a learning exercise,
or perhaps a cautionary tale of how not to write your multiplayer game. Here be dragons.

## How to play

**Enter an arena, choose your avatar, select a spell from each element, and face other players!**

Enter the server IP, create an account, log in. Choose an existing room or create one on your own.

Customize your character and enter the game. Click to walk, press 1-4 to cast spells. Try not to die.

Unfortunately, the game does not have any bots, so you do need some friends to actually test it out.
Is it worth the effort? You decide!

## How to run

### With JARs

Download the JAR archives from the [releases](https://github.com/czyzby/konfigurats/releases).

Make sure that the `42666` and `43666` ports are free.
Place the [`konfigurats-database.script`](server/konfigurats-database.script) in the same
directory as the server JAR. Run the server:

```bash
java -jar konfigurats-server-0.99b.jar
```

Make sure that the server is reachable in your local network or globally.
Now the clients should be able to connect.

To play the game, mark the `konfigurats-0.99b.jar` file as executable and run it with your
Java Runtime Environment. Java 8 and 11 should be fine, newer releases might not be able to
run the game. Enter the server IP after the game is loaded.

### From sources

Clone the project.

```bash
# HTTPS:
git clone https://github.com/czyzby/konfigurats.git
# SSH:
git clone git@github.com:czyzby/konfigurats.git
```

Make sure that the `42666` and `43666` ports are free.
Run the server:

```bash
./gradlew server:run
```

Now the clients should be able to connect.

Run a client from sources:

```bash
./gradlew client:run
```

Export a client JAR from sources:

```bash
./gradlew client:jar
chmod +x client/build/lib/konfigurats-0.99b.jar
```

## The state of the project

The project consists of 3 modules:

* `core`: common libGDX module.
* `client`: desktop/LWJGL3 client that runs the `core` game.
* `server`: a standalone application that handles the game logic and multiplayer capabilities.

Main libraries include:

* **libGDX:** game framework.
* **Box2D:** physics engine.
* **KryoNet:** networking library.
* **HSQLDB:** database.

The project is roughly in the same state as all those years ago, but I did update it a little
so that it's easier to work with. For the curious, this is what I've done:

* Recreated the project with [`gdx-liftoff`](https://github.com/tommyettinger/gdx-liftoff)
  for a modern project structure and updated Gradle scripts.
* Updated from libGDX 1.1 to 1.11. Quite a version bump, but unfortunately there weren't many
  changed APIs.
* Changed the client backend from LWJGL2 to LWJGL3 to avoid errors on newer JVMs.
* Removed `libs/` folders and replaced those with proper Gradle dependencies. Yep.
* Included the server as a Gradle module in the same project to simplify setup.
* Made the "elite" player check optional with environment variables. I think this was some
  rough attempt at monetization that I no longer understand nor care about, just know that
  every player can now play as every character, unless you really want to you can turn it
  on with the `SKIP_ELITE_CHECK=false` environment variable.

I've made no attempt to format, fix, refactor or optimize the code. You can enjoy it in
all its glory. Needless to say, it aged like fine milk.

### What's with the name?

I guess it's sort of a pun on "configuration" and "illuminati"? I don't get it anymore.
This is a non-judgement zone, and I will not question my teenage self.

### Are there any official servers?

Yeah, no. Hosting this thing seems a bit scary.

## Credits

See the in-game "Credits" screen.
Most assets were taken from the [OpenGameArt](https://opengameart.org/) platform.

The code is available under the [CC0 license](LICENSE.txt), and Gods help you if you
decide to use it anywhere. You have been warned.
