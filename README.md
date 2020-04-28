START SHOGI
======

## Introduction

This program is part of Computing Project 3 for CS 1006. It is a Terminal-based (but potentially more flexible) shogi simulator, with primitive AI opponent.

## Dependencies

Software needed:
+ Java 8 or higher
+ An ANSI escape code compatible terminal. Windows cmd will cause printing issues.

The java program build automation tool Gradle, which will not need to be installed here, will take all software dependencies in charge.

## How to build/run it

Run/ build it like this:
+ UNIX(-like) systems: run startShogi shell script.
+ Windows systems: install [gentoo!](https://www.gentoo.org/)
+ For javadoc - run ./gradlew javadoc

## JSON Script properties
+ board - contains components like board height, width, and piece placement.
The characters on the board placement array depend on the piece definitions 
in the piece objects.
+ piece - The object for a piece. char0 and char1 are the characters for each player's 
piece in the board layout array.
	+ placement - how a piece can move - X = absolute moveable position, / = makes a 
	line of possible movement in that direction, O - represents the piece. At least one 
	needed in each placement array. A placement array can be of any size.

## In-game commands
+ If no piece selected:
	+ y-x (x : position x shown on the board) (y: position y show on board)
	select piece at position y-x. Fails if not your piece.
	+ c[index]-\>y-x (y-x position of an empty space) ([index] the index of selected capture piece in capture group)
	drops captured piece no.index to position y-x.
	+ y-x,y-x (both positions) move piece y-x to position y-x 
	+ quit or exit - exits the game
	+ forfeit | give up - you forfeit the game
+ If a piece is selected with y-x command:
	+ y-x - moves selected piece to new position.
	+ cancel - exits selection mode.
	+ All other commands work the same in this mode.


