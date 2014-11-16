# Six2Five

Six2Five downgrades region files from WorldGuard 6 into a format acceptable for
WorldGuard 5, although the task is merely to convert the UUIDs back into names.

## Usage

You can use Six2Five either as a command line program or a program with a GUI.

Double click the .jar to run it with a GUI. Select a regions.yml file to downgrade.

Or instead, run it in the terminal:

	java -jar six2five.jar /path/to/plugins/WorldGuard/world/regions.yml

##Compiling

In terminal, run:

	./gradlew build

Or for Windows users using command prompt:

	gradlew build

## Contributing

We happily accept contributions, especially through pull requests on GitHub.

Submissions must be licensed under the GNU General Public License v3.