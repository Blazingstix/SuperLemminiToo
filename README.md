# SuperLemminiToo
My personal fork of SuperLemmini, which itself is a fork of Lemmini

# SuperLemminiToo v1.40 2021-Nov-15

This is SuperLemminiToo: a fork of SuperLemmini, which is itself a fork of Lemmini.
I made this because there were some minor features I wanted to see in SuperLemmini, which didn't appear to be in the original author's immediate timeline.
I'm not really taking requests, but I'm releasing because I figure some other users may want the same features I did.

# ==USAGE===========

SuperLemminiToo uses the same ini file as SuperLemmini. You can extract this program to its own folder and it will read in all your existing settings from SuperLemmini.

Java 15 or higher is recommended, but the game is compiled against Java 1.8.
Install Instructions: https://docs.oracle.com/en/java/javase/17/install/overview-jdk-installation.html

# ==FEATURES========

Timed Bombers!

The main feature I wanted to restore was Timed Bombers. Not everybody shares this desire (which is why the feature is selectable, under Options), but I feel strongly that in *ORIGINAL* lemmings levels, it is 100% intentional that you need to use a blocker then a bomber if you want to have precise placement of your bombers. Look no further than "Fun 6: A task for blockers and bombers" for proof.
I felt so strongly about it, in fact, that I created this fork.  Everything else changed was just extras once I started looking under the hood.

Visual SFX! 

The oft-forgotten feature added *only* in the "Lemmings for Windows 95" release (AFAIK). Graphics cues are displayed on screen to match the sound effects being played. Most important for when builders have hit their last 3 bricks, but also neat all-around.  I even added some graphics that weren't present in Lemmings95. I may have gone overboard.

Enhanced Status Bar!

Removed clunky full text saying In, Out, Time and replaced with with slick icons. This feature was 1st seen in the SNES and followed about a year later by the SEGA Genesis. Interesting fact, both versions used different icons!  I modeled these after the SNES versions.
And because of the space gained by using icons, I added a 4th icon for number of lemmings needed.
Also, as an extra visual indicator, I made the "In" number Red until you've saved enough Lemmings to pass the level.

Enhanced Icon Bar!

The icon bar has been resized to give a less cramped look. Also the skill counters have been lowered ot be part of the icon buttons now, instead of hovering above them in the black space. The order of icons has changed too, to match the Lemmings for Windows 95 release (which was the last official release for PC, I believe). Now it's Minus, Plus, all 8 skills, then Pause, Fast-Forward, Nuke, Restart, ScrollLock. This pairs like-minded icons together (game speed icons: pause/fast-forward; level restart icons: nuke/restart; and then the scroll lock icon nearest to the map (also farthest away from the others, because it's only applicable for vertical levels... which are specialty levels, and don't even exist in any of the original official Lemmings levels.

Animated Icons!

The icons on the icon bar are now animated!  These are straight from the Lemmings for Windows 95 release (with minimal changes to more easily blend in with different themes)!  Never before seen on modern releases (probably... I dunno)!!  If you have a mod enabled (such as Xmas theme), then the engine will check for animated icons in that mod, then static icons in that mod. Failing to find any mod-specific icons, it will then use animated standard icons, and lastly for the few icons that are not animated (i.e. plus, minus, restart, and vertical scroll lock) it will fall back to the standard static icons.

Icon Labels!

A little bonus feature to go with the new Enhanced Icon Bar. You can toggle on or off text labels to go with all the icons.

All levels unlocked. (Also a toggable option.)

In SuperLemmini this was considered a "cheat" that was enabled with a secret level code.  The "cheat" code also enabled a bunch of special debug features, which I didn't want.  I figure the game's about having fun, so why shouldn't people be able to skip a level they're having difficulty with. Who's to tell them no?
The game still keeps track of levels you've completed or not, so it's still up to you if you want to complete every level.

Minor UI/Gameplay Tweaks.

  Added "Disable Scroll Wheel" option. 
  I found it annoying how my scroll wheel would accidentally change my selected skill when I was trying to middle click.

  Added "Disable Frame Stepping" option.
  Here's where I really started to get a bit nit-picky.
  When paused, if you click anywhere, the game will advance by one frame. Didn't like that feature, so I made it togglable.

  Minor feature, I added some ToolTips to the checkboxes in the Options dialog. I wasn't exactly sure what some options meant at first glance.

# ==THANKS==========

I want to stress that this program was truly written by Volker Oth (Lemmini) and Ryan Sakowski (SuperLemmini), over a combined total of more than twenty years. All I've done is hack a couple lines of code. None of this could be possible without the literally thousands of hours of work done by those two individuals, and their making the source code freely available. Thansk you both for letting me re-live some joy from my childhood in a new way, and for letting me share it with my kids.

Also special thanks to WillLem from the LemmingsForums.net for providing the updated title graphic, and being all around supportive of this endeavour and SuperLemmini in particular.

Special thanks as well to jkapp76 from the LemmingsForums.net for making title icons. I modified them slightly to incorporate them into Icon Labels toggle in SuperLemminiToo.

# ==KNOWN ISSUES====

I've read (but not tested) that Java 15 produces different png files than Java 1.8, so if you've never used SuperLemmini before and need to extract the resources from Lemmings 95 (aka WinLemm), you'll need to do that through the original SuperLemmini.  Then once that's done, you can continue with SuperLemminiToo.  Good news though, you don't need to extract the resources manually anymore -- SuperLemminiToo includes the full root.lzp righti n the zip file.

I didn't test anything related to Replays and External levels, as those didn't personally interest me.