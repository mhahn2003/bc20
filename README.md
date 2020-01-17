# Battlecode 2020 Scaffold

This is the Battlecode 2020 scaffold, containing an `examplefuncsplayer`. Read https://2020.battlecode.org/getting-started!

### Project Structure

- `README.md`
    This file.
- `build.gradle`
    The Gradle build file used to build and run players.
- `src/`
    Player source code.
- `test/`
    Player test code.
- `client/`
    Contains the client.
- `build/`
    Contains compiled player code and other artifacts of the build process. Can be safely ignored.
- `matches/`
    The output folder for match files.
- `maps/`
    The default folder for custom maps.
- `gradlew`, `gradlew.bat`
    The Unix (OS X/Linux) and Windows versions, respectively, of the Gradle wrapper. These are nifty scripts that you can execute in a terminal to run the Gradle build tasks of this project. If you aren't planning to do command line development, these can be safely ignored.
- `gradle/`
    Contains files used by the Gradle wrapper scripts. Can be safely ignored.

### Resources

Here are some useful links for reference.

- Battlecode homepage:
https://2020.battlecode.org/home
- Battlecode repository:
https://github.com/skipiano/bc20
- Game specifications:
https://2020.battlecode.org/specs.html
- Java documentation:
https://2020.battlecode.org/javadoc/index.html
- Debugging:
https://2020.battlecode.org/debugging
- Game visualizer:
https://2020.battlecode.org/visualizer.html
- Bytecode optimization:
https://cory.li/bytecode-hacking/
- Git tutorial:
https://guides.github.com/introduction/git-handbook/
- MIT Battlecode Twitch stream:
https://www.twitch.tv/mitbattlecode
- MIT Battlecode Youtube channel:
https://www.youtube.com/channel/UCOrfTSnyimIXfYzI8j_-CTQ

### Strategy

Our current strategy is to:
1. Produce miners
2. Find the nearest soup deposit and mine them
3. Somehow build a refinery? (Initially we have 200 soup and we used some of them on miners so we somoehow need to get up to 200 more soup until the miners can build them again) (so HQ is a refinery which I missed so we can just refine at HQ)
4. Let the miners keep mining and build landscaper factory (design schools) and drone factory (fullfillment centers)
5. Once with enough landscapers and drones, pick up necessary amounts of landscapers, drones, and one miner
6. Drop off the units once close to enemy HQ (We might need to build net guns in our base to counter this strategy too)
7. Build a net gun very close to the enemy HQ (3 blocks)
8. Surround enemy HQ with landscapers and bury to kill HQ
9. If there are units surrounding HQ so we can't bury HQ, build a river and flood HQ.

1. Surround enemy with walls
2. Put cows inside walls with drones
3. Install net guns to ward off drones repicking cows
4. Guard wall with landscapers?

### Need to Fix

1. Improve scouting mechanism of searching for soup
2. How to check if the area is inacessible without landscapers?
3. Create path objects each with an id(for future modification, need to be set unique in some way), so if nothing happend, just render the path
4. Install net guns near miners to protect them
11. Build net guns next to refineries?
20. Sometimes drones aren't dropping units off? I think it's because it keeps trying to drop in Direction.CENTER
21. Optimize miner code
22. Program miners so they never come into HQ after outer layer is built
23. When refinery is destroyed removed from refineryLoc
24. Make miners remove HQLoc from nearest refinery after outerlayer is built
25. Check all 4 directions on where the design school can be placed
26. Check if there's any height 1- nearby hq, if so, just 3x3 turtle immediately
21. disruptWithCow strat: make a 3x5 box right next to HQ, and place cows in it. Should look something like this:
LNL
LC
 C
 C
LNL

Hopefully the landscapers are enough to protect the net guns from getting buried.
Install net guns to protect the landscapers, and landscapers protect the net guns from getting potentially buried by enemy landscapers. The landscapers dig a pit where the cows are going to be, and drones come and place the cows in there. 