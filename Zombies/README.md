# Zombies

### Summary

You're locked in a mansion with your friends. You have k friends and there are k entry points. 

There are zombies outsides and you're in charge of eliminating them while your friends are in charge of letting the zombies in.

Your friends have a certain success rate at letting zombies inside at a certain time interval.

You also have a certain success rate at eliminating a zombie at a certain time interval.

Each of your friends keeps tracks of how many zombies they've let inside in total.

If there are too many zombies inside, you need to contact your friends individually.

They will then stop letting zombies until you've reached a threshodld.

i.e. You tell them when to let zombies inside again.

In this story, you and your friends are represented by threads.

### Parameters

You need to pass in two arguments.

k which represents the number of friends that you want, i.e. the number of threads.

n which represents the maximum number of zombies that you want to let inside.
